package net.minecraftforge.oredict;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static net.minecraftforge.oredict.OreDictionary.*;

@EventBusSubscriber(modid = "forge.testcapmod")
public class OreDictToAspectConverter
{
    private static final Pattern firstLowercaseWord = Pattern.compile("([a-z]+)[A-Z]");
    
    @SubscribeEvent
    public static void onItemstackCapability(AttachCapabilitiesEvent<ItemStack> event)
    {
    	if (event.getObject().isEmpty()) return;
    	
    	List<String> names = Arrays.stream(getOreIDs(event.getObject())).mapToObj(OreDictionary::getOreName).collect(Collectors.toList());
    	ItemAspects.Builder builder = new ItemAspects.Builder();
    	Matcher matcher = firstLowercaseWord.matcher("");
    	for (String s : names) 
    	{
    		matcher.reset(s);
    		if (matcher.find())
    		{
    			builder.withAspect("shape").withValue(matcher.group(1)).next()
    				   .withAspect("material").withValue(s.replace(matcher.group(1), "").toLowerCase(Locale.US)).next();
    		}
    		else
    		{
    			builder.withAspect("material").withValue(s).next();
    		}
    	}
    	
    	final IItemAspects aspects = builder.build();
    	event.addCapability(new ResourceLocation("forge:itemaspects"), new ICapabilityProvider()
		{
			
			@Override
			public boolean hasCapability(Capability<?> capability, EnumFacing facing)
			{
				return capability == CapabilityItemAspects.ITEM_ASPECTS_CAPABILITY;
			}
			
			@Override
			public <T> T getCapability(Capability<T> capability, EnumFacing facing)
			{
				return capability == CapabilityItemAspects.ITEM_ASPECTS_CAPABILITY ? CapabilityItemAspects.ITEM_ASPECTS_CAPABILITY.cast(aspects) : null;
			}
		});
    }
    
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event)
    {
    	IItemAspects aspects = event.getItemStack().getCapability(CapabilityItemAspects.ITEM_ASPECTS_CAPABILITY, null);
    	if (aspects.getAspects().isEmpty()) return;
    	
    	event.getToolTip().add("Aspects:");
    	for (String s : aspects.getAspects())
    	{
    		event.getToolTip().add("  " + s + ":");
    		for (String value : aspects.getValues(s))
    		{
    			event.getToolTip().add("    - " + value);
    		}
    	}
    }
}
