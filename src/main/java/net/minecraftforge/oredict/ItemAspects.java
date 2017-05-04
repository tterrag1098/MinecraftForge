package net.minecraftforge.oredict;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

public class ItemAspects implements IItemAspectsModifiable
{
	public static class Builder
	{
		// Use set values for O(1) contains() calls
		private final Multimap<String, String> aspects = MultimapBuilder.hashKeys().hashSetValues().build();

		public IItemAspects build()
		{
			return new ItemAspects(aspects);
		}
		
		public AspectBuilder withAspect(String aspect)
		{
			return new AspectBuilder(aspect);
		}
		
		private Builder withAspect(AspectBuilder builder)
		{
			this.aspects.putAll(builder.name, builder.values);
			return this;
		}
		
		public class AspectBuilder
		{
			private final String name;
			private final Set<String> values = new HashSet<>();

			private AspectBuilder(String name)
			{
				this.name = name;
			}

			public AspectBuilder withValue(String value)
			{
				this.values.add(value);
				return this;
			}

			public Builder next()
			{
				return Builder.this.withAspect(this);
			}
		}
	}
	
	public static final IItemAspects EMPTY = new ItemAspects(HashMultimap.create());

	private final Multimap<String, String> aspects;

	private ItemAspects(Multimap<String, String> aspects)
	{
		this.aspects = aspects;
	}

	@Override
	public boolean hasAspect(String aspect, String name)
	{
		return aspects.get(aspect).contains(name);
	}

	@Override
	public Collection<String> getValues(String aspect)
	{
		return ImmutableSet.copyOf(aspects.get(aspect));
	}

	@Override
	public Collection<String> getAspects()
	{
		return ImmutableSet.copyOf(aspects.keySet());
	}

	@Override
	public void addValue(String aspect, String value)
	{
		this.aspects.put(aspect, value);
	}
}
