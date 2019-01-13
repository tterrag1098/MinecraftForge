/*
 * Minecraft Forge
 * Copyright (c) 2016-2018.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.common;

import static com.electronwill.nightconfig.core.ConfigSpec.CorrectionAction.ADD;
import static com.electronwill.nightconfig.core.ConfigSpec.CorrectionAction.REMOVE;
import static com.electronwill.nightconfig.core.ConfigSpec.CorrectionAction.REPLACE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.InMemoryFormat;
import com.electronwill.nightconfig.core.utils.UnmodifiableConfigWrapper;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionAction;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionListener;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/*
 * Like {@link com.electronwill.nightconfig.core.ConfigSpec} except in builder format, and extended to acept comments, language keys,
 * and other things Forge configs would find useful.
 */

public class ForgeConfigSpec extends UnmodifiableConfigWrapper<Config>
{
    private Map<List<String>, String> levelComments = new HashMap<>();
    private ForgeConfigSpec(Config storage, Map<List<String>, String> levelComments) {
        super(storage);
        this.levelComments = levelComments;
    }

    public boolean isCorrect(CommentedConfig config) {
        return correct(this.config, config, null, null, null, true) == 0;
    }

    public int correct(CommentedConfig config) {
        return correct(config, (action, path, incorrectValue, correctedValue) -> {});
    }
    public int correct(CommentedConfig config, CorrectionListener listener) {
        LinkedList<String> parentPath = new LinkedList<>(); //Linked list for fast add/removes
        return correct(this.config, config, parentPath, Collections.unmodifiableList(parentPath), listener, false);
    }

    private int correct(Config spec, CommentedConfig config, LinkedList<String> parentPath, List<String> parentPathUnmodifiable, CorrectionListener listener, boolean dryRun)
    {
        int count = 0;

        Map<String, Object> specMap = spec.valueMap();
        Map<String, Object> configMap = config.valueMap();

        for (Map.Entry<String, Object> specEntry : specMap.entrySet())
        {
            final String key = specEntry.getKey();
            final Object specValue = specEntry.getValue();
            final Object configValue = configMap.get(key);
            final CorrectionAction action = configValue == null ? ADD : REPLACE;

            if (!dryRun)
                parentPath.addLast(key);

            if (specValue instanceof Config)
            {
                if (configValue instanceof CommentedConfig)
                {
                    count += correct((Config)specValue, (CommentedConfig)configValue, parentPath, parentPathUnmodifiable, listener, dryRun);
                    if (count > 0 && dryRun)
                        return count;
                }
                else if (dryRun)
                {
                    return 1;
                }
                else
                {
                    CommentedConfig newValue = config.createSubConfig();
                    configMap.put(key, newValue);
                    listener.onCorrect(action, parentPathUnmodifiable, configValue, newValue);
                    count++;
                    count += correct((Config)specValue, newValue, parentPath, parentPathUnmodifiable, listener, dryRun);
                }

                String newComment = levelComments.get(parentPath);
                String oldComment = config.getComment(key);
                if (!Objects.equals(oldComment, newComment))
                {
                    if (dryRun)
                        return 1;

                    //TODO: Comment correction listener?
                    config.setComment(key, newComment);
                }
            }
            else
            {
                ValueSpec valueSpec = (ValueSpec)specValue;
                if (!valueSpec.test(configValue))
                {
                    if (dryRun)
                        return 1;

                    Object newValue = valueSpec.correct(configValue);
                    configMap.put(key, newValue);
                    listener.onCorrect(action, parentPathUnmodifiable, configValue, newValue);
                    count++;
                }
                String oldComment = config.getComment(key);
                if (!Objects.equals(oldComment, valueSpec.getComment()))
                {
                    if (dryRun)
                        return 1;

                    //TODO: Comment correction listener?
                    config.setComment(key, valueSpec.getComment());
                }
            }
            if (!dryRun)
                parentPath.removeLast();
        }

        // Second step: removes the unspecified values
        for (Iterator<Map.Entry<String, Object>> ittr = configMap.entrySet().iterator(); ittr.hasNext();)
        {
            Map.Entry<String, Object> entry = ittr.next();
            if (!specMap.containsKey(entry.getKey()))
            {
                if (dryRun)
                    return 1;

                ittr.remove();
                parentPath.addLast(entry.getKey());
                listener.onCorrect(REMOVE, parentPathUnmodifiable, entry.getValue(), null);
                parentPath.removeLast();
                count++;
            }
        }
        return count;
    }
    
    private static <T> Supplier<T> supply(T val) { return () -> val; }
    
    private static Predicate<Object> defaultValidator(Supplier<?> supplier) { 
        return o -> o != null && supplier.get().getClass().isAssignableFrom(o.getClass());
    }

    public static class Builder
    {
        private final Config storage = InMemoryFormat.withUniversalSupport().createConfig();
        private Map<List<String>, String> levelComments = new HashMap<>();
        private LinkedList<String> currentPath = new LinkedList<>();
        
        Builder define(ValueBuilder<?> value) {
            push(value.path);
            storage.set(path(), new ValueSpec(value));
            comment(value.comment);
            pop(value.path.size());
            return this;
        }

        //Object
        public <T> ValueBuilder<T> value(String path, T defaultValue) {
            return value(split(path), defaultValue);
        }
        @SuppressWarnings("unchecked")
        public <T> ValueBuilder<T> value(List<String> path, T defaultValue) {
            return value(path, supply(defaultValue), (Class<? extends T>) defaultValue.getClass());
        }
        public <T> ValueBuilder<T> value(List<String> path, Supplier<T> defaultSupplier, Class<? extends T> clazz) {
            return new ValueBuilder<>(this, path, defaultSupplier, clazz);
        }

        // Comparable (allows range)
        @SuppressWarnings("unchecked")
        public <V extends Comparable<? super V>> ComparableValueBuilder<V> comparableValue(String path, V defaultValue) {
            return comparableValue(split(path), defaultValue, (Class<V>) defaultValue.getClass());
        }
        public <V extends Comparable<? super V>> ComparableValueBuilder<V> comparableValue(String path, V defaultValue, Class<V> clazz) {
            return comparableValue(split(path), defaultValue, clazz);
        }
        public <V extends Comparable<? super V>> ComparableValueBuilder<V> comparableValue(List<String> path, V defaultValue, Class<V> clazz) {
            return comparableValue(path, (Supplier<V>)() -> defaultValue, clazz);
        }
        public <V extends Comparable<? super V>> ComparableValueBuilder<V> comparableValue(String path, Supplier<V> defaultSupplier, Class<V> clazz) {
            return comparableValue(split(path), defaultSupplier, clazz);
        }
        public <V extends Comparable<? super V>> ComparableValueBuilder<V> comparableValue(List<String> path, Supplier<V> defaultSupplier, Class<V> clazz) {
            return new ComparableValueBuilder<>(this, path, defaultSupplier, clazz);
        }
        
        // Enumerated, validator is automatically created from given acceptable values
        public <T> ValueBuilder<T> enumeratedValue(String path, T defaultValue, Collection<? extends T> acceptableValues) {
            return enumeratedValue(split(path), defaultValue, acceptableValues);
        }
        public <T> ValueBuilder<T> enumeratedValue(String path, Supplier<T> defaultSupplier, Class<? extends T> clazz, Collection<? extends T> acceptableValues) {
            return enumeratedValue(split(path), defaultSupplier, clazz, acceptableValues);
        }
        @SuppressWarnings("unchecked")
        public <T> ValueBuilder<T> enumeratedValue(List<String> path, T defaultValue, Collection<? extends T> acceptableValues) {
            return enumeratedValue(path, supply(defaultValue), (Class<? extends T>) defaultValue.getClass(), acceptableValues);
        }
        public <T> ValueBuilder<T> enumeratedValue(List<String> path, Supplier<T> defaultSupplier, Class<? extends T> clazz, Collection<? extends T> acceptableValues) {
            return value(path, defaultSupplier, clazz).validator(acceptableValues::contains);
        }
        
        // List special case, allows easy validation of list objects
        public <T> ValueBuilder<List<? extends T>> listValue(String path, List<? extends T> defaultValue, Predicate<T> elementValidator) {
            return listValue(split(path), defaultValue, elementValidator);
        }
        public <T> ValueBuilder<List<? extends T>> listValue(String path, Supplier<List<? extends T>> defaultSupplier, Predicate<T> elementValidator) {
            return listValue(split(path), defaultSupplier, elementValidator);
        }
        public <T> ValueBuilder<List<? extends T>> listValue(List<String> path, List<? extends T> defaultValue, Predicate<T> elementValidator) {
            return listValue(path, supply(defaultValue), elementValidator);
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <T> ValueBuilder<List<? extends T>> listValue(List<String> path, Supplier<List<? extends T>> defaultSupplier, Predicate<T> elementValidator) {
            return value(path, defaultSupplier, (Class<List<T>>) (Class) List.class).corrector(value -> {
                if (value == null || !(value instanceof List) || ((List<?>)value).isEmpty()) {
                    return defaultSupplier.get();
                }
                List<? extends T> list = Lists.newArrayList((List<? extends T>) value);
                Iterator<? extends T> iter = list.iterator();
                while (iter.hasNext()) {
                    T ele = iter.next();
                    if (!elementValidator.test(ele)) {
                        iter.remove();
                    }
                }
                if (list.isEmpty()) {
                    return defaultSupplier.get();
                }
                return list;
            });
        }

        // Enum, proxies to comparableValue
        public <V extends Enum<V>> ComparableValueBuilder<V> enumValue(String path, V defaultValue) {
            return enumValue(split(path), defaultValue);
        }
        public <V extends Enum<V>> ComparableValueBuilder<V> enumValue(List<String> path, V defaultValue) {
            return enumValue(path, defaultValue, defaultValue.getDeclaringClass().getEnumConstants());
        }
        public <V extends Enum<V>> ComparableValueBuilder<V> enumValue(String path, V defaultValue, @SuppressWarnings("unchecked") V... acceptableValues) {
            return enumValue(split(path), defaultValue, acceptableValues);
        }
        public <V extends Enum<V>> ComparableValueBuilder<V> enumValue(List<String> path, V defaultValue, @SuppressWarnings("unchecked") V... acceptableValues) {
            return enumValue(path, defaultValue, Arrays.asList(acceptableValues));
        }
        public <V extends Enum<V>> ComparableValueBuilder<V> enumValue(String path, V defaultValue, Collection<V> acceptableValues) {
            return enumValue(split(path), defaultValue, acceptableValues);
        }
        public <V extends Enum<V>> ComparableValueBuilder<V> enumValue(List<String> path, V defaultValue, Collection<V> acceptableValues) {
            return enumValue(path, defaultValue, acceptableValues::contains);
        }
        public <V extends Enum<V>> ComparableValueBuilder<V> enumValue(String path, V defaultValue, Predicate<Object> validator) {
            return enumValue(split(path), defaultValue, validator);
        }
        public <V extends Enum<V>> ComparableValueBuilder<V> enumValue(List<String> path, V defaultValue, Predicate<Object> validator) {
            return enumValue(path, () -> defaultValue, validator, defaultValue.getDeclaringClass());
        }
        public <V extends Enum<V>> ComparableValueBuilder<V> enumValue(String path, Supplier<V> defaultSupplier, Predicate<Object> validator, Class<V> clazz) {
            return enumValue(split(path), defaultSupplier, validator, clazz);
        }
        public <V extends Enum<V>> ComparableValueBuilder<V> enumValue(List<String> path, Supplier<V> defaultSupplier, Predicate<Object> validator, Class<V> clazz) {
            return (ComparableValueBuilder<V>) comparableValue(path, defaultSupplier, clazz).validator(validator);
        }

        // boolean special case, handle validation of string equivalents
        public ValueBuilder<Boolean> value(String path, boolean defaultValue) {
            return value(split(path), defaultValue);
        }
        public ValueBuilder<Boolean> value(List<String> path, boolean defaultValue) {
            return value(path, (Supplier<Boolean>)() -> defaultValue);
        }
        public ValueBuilder<Boolean> value(String path, Supplier<Boolean> defaultSupplier) {
            return value(split(path), defaultSupplier);
        }
        public ValueBuilder<Boolean> value(List<String> path, Supplier<Boolean> defaultSupplier) {
            return value(path, defaultSupplier, Boolean.class).validator(o -> {
                if (o instanceof String) return ((String)o).equalsIgnoreCase("true") || ((String)o).equalsIgnoreCase("false");
                return o instanceof Boolean;
            });
        }
        
        List<String> path() {
            List<String> path = new ArrayList<String>(currentPath);
            Collections.reverse(path); // Flip stack
            return path;
        }
        
        public Builder comment(String... comments) {
            levelComments.put(path(), LINE_JOINER.join(comments));
            return this;
        }

        public Builder push(String path) {
            return push(split(path));
        }

        public Builder push(List<String> path) {
            path.forEach(currentPath::push);
            return this;
        }

        public Builder pop() {
            return pop(1);
        }

        public Builder pop(int count) {
            if (count > currentPath.size())
                throw new IllegalArgumentException("Attempted to pop " + count + " elements when we only had: " + currentPath);
            for (int x = 0; x < count; x++)
                currentPath.pop();
            return this;
        }

        public ForgeConfigSpec build()
        {
            return new ForgeConfigSpec(storage, levelComments);
        }
    }
    
    public static class ValueBuilder<T>
    {
        private final Builder parent;
        final List<String> path;
        private final Supplier<T> supplier;
        final Class<? extends T> clazz;
        
        String[] comment = new String[0];
        String langKey;
        boolean worldRestart = false;
        
        Predicate<?> validator;
        UnaryOperator<? super T> corrector;
        
        ValueBuilder(Builder parent, List<String> path, Supplier<T> defaultSupplier, Class<? extends T> clazz)
        {
            this.parent = parent;
            this.path = path;
            this.supplier = defaultSupplier;
            this.clazz = clazz;
            this.validator = defaultValidator(defaultSupplier);
            this.corrector = $ -> defaultSupplier.get();
        }

        public ValueBuilder<T> comment(String... comment)
        {
            this.comment = comment;
            return this;
        }

        public ValueBuilder<T> translation(String translationKey)
        {
            this.langKey = translationKey;
            return this;
        }
          
        public ValueBuilder<T> worldRestart()
        {
            worldRestart = true;
            return this;
        }
        
        public ValueBuilder<T> validator(Predicate<?> validator)
        {
            this.validator = validator;
            return this;
        }
        
        public ValueBuilder<T> corrector(UnaryOperator<? super T> corrector)
        {
            this.corrector = corrector;
            return this;
        }
        
        public Builder define()
        {
            return this.parent.define(this);
        }

        void ensureEmpty()
        {
            validate(comment, "Non-null comment when null expected");
            validate(langKey, "Non-null translation key when null expected");
            validate(worldRestart, "Dangeling world restart value set to true");
        }
        
        Range<? extends T> getRange()
        {
            return null;
        }

        void validate(Object value, String message)
        {
            if (value != null)
                throw new IllegalStateException(message);
        }
        
        void validate(boolean value, String message)
        {
            if (value)
                throw new IllegalStateException(message);
        }
    }
    
    public static class ComparableValueBuilder<T extends Comparable<? super T>> extends ValueBuilder<T>
    {  
        private Range<T> range;

        ComparableValueBuilder(Builder parent, List<String> path, Supplier<T> defaultSupplier, Class<? extends T> clazz)
        {
            super(parent, path, defaultSupplier, clazz);
        }

        private ValueBuilder<T> range(Range<T> range)
        {
            this.range = range;
            return this;
        }
        
        @SuppressWarnings("unchecked")
        public ValueBuilder<T> range(T min, T max)
        {
            if (min.compareTo(max) > 0)
                throw new IllegalArgumentException("Range min most be less then max.");
            return range(new Range<T>((Class<T>) clazz, min, max));
        }

        @Override
        void ensureEmpty()
        {
            super.ensureEmpty();
            validate(range, "Non-null range when null expected");
        }
    }

    @SuppressWarnings("unused")
    private static class Range<V extends Comparable<? super V>> implements Predicate<Object>
    {
        private final  Class<V> clazz;
        private final V min;
        private final V max;

        private Range(Class<V> clazz, V min, V max)
        {
            this.clazz = clazz;
            this.min = min;
            this.max = max;
        }

        public Class<V> getClazz() { return clazz; }
        public V getMin() { return min; }
        public V getMax() { return max; }

        @Override
        public boolean test(Object t)
        {
            if (!clazz.isInstance(t)) return false;
            V c = clazz.cast(t);
            return c.compareTo(min) >= 0 && c.compareTo(max) <= 0;
        }
    }

    public static class ValueSpec
    {
        private final String comment;
        private final String langKey;
        private final Range<?> range;
        private final boolean worldRestart;
        private final Class<?> clazz;
        private final Supplier<?> supplier;
        private final Predicate<Object> validator;
        private final UnaryOperator<Object> corrector;
        private Object _default = null;

        @SuppressWarnings("unchecked")
        private ValueSpec(ValueBuilder<?> context)
        {
            Objects.requireNonNull(context.supplier, "Default supplier can not be null");
            Objects.requireNonNull(context.validator, "Validator can not be null");
            Objects.requireNonNull(context.corrector, "Corrector can not be null");

            this.comment = context.comment == null ? null : LINE_JOINER.join(context.comment);
            this.langKey = context.langKey;
            this.range = context.getRange();
            this.worldRestart = context.worldRestart;
            this.clazz = context.clazz;
            this.supplier = context.supplier;
            this.validator = (Predicate<Object>) context.validator;
            this.corrector = (UnaryOperator<Object>) context.corrector;
        }

        public String getComment() { return comment; }
        public String getTranslationKey() { return langKey; }
        @SuppressWarnings("unchecked")
        public <V extends Comparable<? super V>> Range<V> getRange() { return (Range<V>)this.range; }
        public boolean needsWorldRestart() { return this.worldRestart; }
        public Class<?> getClazz(){ return this.clazz; }
        public boolean test(Object value) { return validator.test(value); }
        public Object correct(Object value) { return corrector.apply(value); }

        public Object getDefault()
        {
            if (_default == null)
                _default = supplier.get();
            return _default;
        }
    }

    private static final Joiner LINE_JOINER = Joiner.on("\n");
    private static final Splitter DOT_SPLITTER = Splitter.on(".");
    private static List<String> split(String path)
    {
        return Lists.newArrayList(DOT_SPLITTER.split(path));
    }
}
