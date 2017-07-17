/**
 * these objects in here are almost all very elegant as lambdas in an enum or static in an interface HOWEVER, they are
 * here in individual java source files for various reasons:
 * <ol>
 * <li>parser traits via annotations and/or marker interfaces</li>
 * <li>cheapest informative toString</li>
 * <li>passing by enum confuses clinit and/or unknown pure-evil inits in my source code</li>
 * </ol>
 */
package bbcursive.lib;

