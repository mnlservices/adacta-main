package nl.defensie.adacta.security;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import net.sf.acegisecurity.ConfigAttribute;
import net.sf.acegisecurity.ConfigAttributeDefinition;
import net.sf.acegisecurity.SecurityConfig;
import net.sf.acegisecurity.intercept.method.MethodDefinitionMap;
import net.sf.acegisecurity.intercept.method.aopalliance.MethodSecurityInterceptor;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.stereotype.Component;

/**
 * <p>Applies the method security definitions defined in {@link #methodSecurityDefinitions} on the 
 * {@link #onInterceptor}, specifically overriding just the keys which exist in 
 * {@link #methodSecurityDefinitions}. Other configuration is left intact.</p>
 * <p>Due to the closed nature of the acegi-security API, this class constructs the 
 * {@link Method} targeted by a security definition from a given String. It 
 * supports only the following two constructs:
 * <ul>
 * <li><code>nl.package.class.method</code> &nbsp;&nbsp;to match all methods of a name in a class, regardless of parameters</li>
 * <li><code>nl.package.class.*</code> &emsp;&emsp;&emsp;&nbsp; to match all methods in a class</li>
 * </ul></p>
 * <p>For more specific requirements, this class can be extended.</p>
 *  
 * @author Mark Tielemans
 */
@Component
public class MethodInterceptorSecurityOverrider {

    private static final Logger logger = Logger.getLogger(MethodInterceptorSecurityOverrider.class);

    public static void logInterceptorSecurityConfig(final MethodSecurityInterceptor interceptor) throws BeansException {
        if (!logger.isDebugEnabled()) {
            return;
        }
        
        // Log the security definitions now on onInterceptor
        final MethodDefinitionMap mdSource = (MethodDefinitionMap) interceptor.getObjectDefinitionSource();

        @SuppressWarnings("unchecked")
        final Iterator<ConfigAttributeDefinition> definitions = mdSource.getConfigAttributeDefinitions();
        logger.debug("Security config definitions now on " + interceptor);
        
        int i = 0;
        while (definitions.hasNext()) {
            final ConfigAttributeDefinition definition = definitions.next();

            logger.debug("Definition-" + i);
            
            @SuppressWarnings("unchecked")
            final Iterator<ConfigAttribute> definitionAttributes = definition.getConfigAttributes();
            while (definitionAttributes.hasNext()) {
                final ConfigAttribute att = definitionAttributes.next();
                logger.debug("SecurityConfigs: " + att.getAttribute());
            }
            
            i++;
        }
    }
    
    /** The security definitions per class method */
    private Map<String, String> methodSecurityDefinitions;

    /** The interceptor to override security definitions on */
    private MethodSecurityInterceptor onInterceptor;
    
    /**
     * Create a new overrider that overrides values on given <b>onInterceptor</b>.
     * @param onInterceptor
     */
    public MethodInterceptorSecurityOverrider(final MethodSecurityInterceptor onInterceptor) {
        this.onInterceptor = onInterceptor;
    }
    
    /**
     * <p>Add the given <b>securityConfigAttributes</b> on all methods that match the 
     * <b>className</b> and <b>methodName</b> using this instance's interceptor.</p>
     * <p>Multiple security config attributes in <b>securityConfigAttributes</b> must be 
     * comma-separated.</p>
     * @param className
     * @param methodName
     * @param securityConfigAttributes
     * @throws ClassNotFoundException
     */
    public void addMethodConfig(final String className, final String methodName, final String securityConfigAttributes) throws ClassNotFoundException {
        // This is the security config value
        final ConfigAttributeDefinition cad = getSecurityDefinition(securityConfigAttributes);
        logger.debug("Created configAttributeDefinition from securityConfigAttributes");
        
        // These are the methods to which the given security configs will be applied
        final List<Method> onMethods = getMatchingMethods(className, methodName);
        
        for (final Method method : onMethods) {
            ((MethodDefinitionMap) this.onInterceptor.getObjectDefinitionSource()).addSecureMethod(method, cad);
            logger.debug("Added security definitions on " + method.getName());
        }
    }
    
    protected List<Method> getMatchingMethods(final String className, final String methodName) throws ClassNotFoundException {
        final Class<?> clazz = Class.forName(className);
        
        if ("*".equals(methodName)) {
            logger.debug("Matching all methods in " + className);
            return Arrays.asList(clazz.getDeclaredMethods());
        }
        
        final List<Method> methods = new ArrayList<Method>();
        
        for (final Method declaredMethod : clazz.getDeclaredMethods()) {
            if (declaredMethod.getName().equals(methodName) || "*".equals(methodName)) {
                logger.debug("Matching " + declaredMethod + " in " + className);
                methods.add(declaredMethod);
            }
        }
        
        return methods;
    }
    
    protected ConfigAttributeDefinition getSecurityDefinition(final String securityDefinitionAttributes) {
        final ConfigAttributeDefinition configAttributeDefinition = new ConfigAttributeDefinition();
        
        for (final String securityDefinitionAttribute : securityDefinitionAttributes.split(Pattern.quote(","))) {
            logger.debug("Security definition attribute: " + securityDefinitionAttribute);
            configAttributeDefinition.addConfigAttribute(new SecurityConfig(securityDefinitionAttribute));
        }
        
        return configAttributeDefinition;
    }
    
    public void init() throws ClassNotFoundException {
        for (final Entry<String, String> entry : methodSecurityDefinitions.entrySet()) {
            final String classAndMethod = entry.getKey();
            final String securityConfigDef = entry.getValue();
            
            final String[] path = classAndMethod.split(Pattern.quote("."));
            if (path.length <= 1) {
                throw new IllegalArgumentException(classAndMethod + " does not identify a class and method(s)");
            }
            
            final String className = classAndMethod.substring(0, classAndMethod.lastIndexOf("."));
            final String methodName = path[path.length - 1];
            logger.debug("Adding method security definition, class=" + className + ", method=" + methodName
                    + ", securityDefinition=" + securityConfigDef);
            
            try {
                addMethodConfig(className, methodName, securityConfigDef);
                logger.debug("Security definition " + securityConfigDef + " added on " + classAndMethod);
            } catch (final ClassNotFoundException e) {
                logger.error("Failed to add security config for " + classAndMethod, e);
                throw e;
            }
        }
        
        logger.debug("Init complete");
        
        logInterceptorSecurityConfig(this.onInterceptor);
    }
    
    public void setMethodSecurityDefinitions(final Map<String, String> methodSecurityDefinitions) {
        this.methodSecurityDefinitions = methodSecurityDefinitions;
        logger.debug("Method security definitions set");
    }
}
