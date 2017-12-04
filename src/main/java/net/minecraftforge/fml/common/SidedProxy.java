/*
 * Minecraft Forge
 * Copyright (c) 2016.
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

package net.minecraftforge.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Sided proxies are loaded based on the specific environment they find themselves loaded into.
 * They are used to ensure that client-specific code (such as GUIs) is only loaded into the game
 * on the client side.
 * It is applied to static fields of a class, anywhere in your mod code. FML will scan
 * and load any classes with this annotation at mod construction time.
 *
 * <p>
 * This example will load a CommonProxy on the server side, and a ClientProxy on the client side.
 *
 * <pre>
 *  public class MySidedProxyHolder {
 *      {@literal @}SidedProxy(modId="mymodid")
 *      public static CommonProxy proxy;
 *  }
 *
 *  {@literal @}ProxyClass(modId = "mymodid", side = Side.SERVER)
 *  public class CommonProxy {
 *      // Common or server stuff here that needs to be overridden on the client
 *  }
 *  
 *  {@literal @}ProxyClass(modId = "mymodid", side = Side.CLIENT)
 *  public class ClientProxy extends CommonProxy {
 *      // Override common stuff with client specific stuff here
 *  }
 * </pre>
 * @author cpw
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SidedProxy
{
    /**
     * The full name of the client side class to load and populate.
     * Defaults to the nested class named "ClientProxy" in the current class.
     * 
     * @deprecated Use {@link ProxyClass}
     */
    @Deprecated
    String clientSide() default "";

    /**
     * The full name of the server side class to load and populate.
     * Defaults to the nested class named "ServerProxy" in the current class.
     * 
     * @deprecated Use {@link ProxyClass}
     */
    @Deprecated
    String serverSide() default "";

    /**
     * The name of a mod to load this proxy for. This is required if this annotation is not in the class with @Mod annotation.
     * Or there is no other way to determine the mod this annotation belongs to. When in doubt, add this value.
     */
    String modId() default "";

    /**
     * Use this to define your client and server proxy implementations.
     * <p>
     * {@code @ProxyClass(modId = "mymod", side = Side.CLIENT)}<br>
     * or<br>
     * {@code @ProxyClass(modId = "mymod", side = Side.SERVER)}
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ProxyClass {

        String modId();

        Side side();
    }
}
