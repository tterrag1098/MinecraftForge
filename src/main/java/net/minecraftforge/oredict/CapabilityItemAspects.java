package net.minecraftforge.oredict;

import java.util.concurrent.Callable;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.Constants;

public class CapabilityItemAspects {

	    @CapabilityInject(IItemAspects.class)
	    public static Capability<IItemAspects> ITEM_ASPECTS_CAPABILITY = null;

	    public static void register()
	    {
	        CapabilityManager.INSTANCE.register(IItemAspects.class, new Capability.IStorage<IItemAspects>()
	        {
	            @Override
	            public NBTBase writeNBT(Capability<IItemAspects> capability, IItemAspects instance, EnumFacing side)
	            {
	                NBTTagList nbtTagList = new NBTTagList();
	                for (String aspect : instance.getAspects()) 
	                {
	                	NBTTagCompound aspectTag = new NBTTagCompound();
	                	aspectTag.setString("name", aspect);
	                	NBTTagList valueList = new NBTTagList();
	                	for (String value : instance.getValues(aspect))
	                	{
	                		valueList.appendTag(new NBTTagString(value));
	                	}
	                	aspectTag.setTag("values", valueList);
	                	nbtTagList.appendTag(aspectTag);
	                }
	                return nbtTagList;
	            }

	            @Override
	            public void readNBT(Capability<IItemAspects> capability, IItemAspects instance, EnumFacing side, NBTBase base)
	            {
	                if (!(instance instanceof IItemAspectsModifiable))
	                    throw new RuntimeException("IItemAspects instance does not implement IItemAspectsModifiable");
	                IItemAspectsModifiable itemAspectsModifiable = (IItemAspectsModifiable) instance;
	                NBTTagList tagList = (NBTTagList) base;
	                for (int i = 0; i < tagList.tagCount(); i++)
	                {
	                   	NBTTagCompound aspectTag = tagList.getCompoundTagAt(i);
	                    String name = aspectTag.getString("name");
	                    NBTTagList valueList = aspectTag.getTagList("values", Constants.NBT.TAG_STRING);
	                    for (int j = 0; j < valueList.tagCount(); j++)
	                    {
	                    	itemAspectsModifiable.addValue(name, valueList.getStringTagAt(i));
	                    }
	                }
	            }
	        }, new Callable<ItemAspects>()
	        {
	            @Override
	            public ItemAspects call() throws Exception
	            {
	                return (ItemAspects) new ItemAspects.Builder().build();
	            }
	        });
	    }
}
