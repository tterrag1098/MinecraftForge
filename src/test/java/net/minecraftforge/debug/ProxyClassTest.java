package net.minecraftforge.debug;

import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy.ProxyClass;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = ProxyClassTest.MODID, name = "Proxy class test mod", version = "1.0", acceptableRemoteVersions = "*")
public class ProxyClassTest
{
    public static final String MODID = "proxy_class_test";

    @SidedProxy
    public static ProxyBase proxy = null;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        proxy.preInit(event.getModLog());
        assert (proxy.getClass() == ProxyBase.class) == (event.getSide().isServer()) : "Proxy lookup broken!";
    }

    @ProxyClass(modId = MODID, side = Side.SERVER)
    public static class ProxyBase
    {

        public void preInit(Logger logger)
        {
            logger.info("We're on a dedicated server!");
        }
    }

    @ProxyClass(modId = MODID, side = Side.CLIENT)
    public static final class ProxyClient extends ProxyBase
    {

        @Override
        public void preInit(Logger logger)
        {
            logger.info("We're on a client!");
        }
    }
}
