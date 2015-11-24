package jeeves.server.overrides;

import java.util.Properties;

import jeeves.config.springutil.GeonetworkFilterSecurityInterceptor;

import org.jdom.Element;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.util.RegexRequestMatcher;

class AddInterceptUrlUpdater extends BeanUpdater {

		private String patternStr;
    private RegexRequestMatcher pattern;
    private String access;

    public AddInterceptUrlUpdater(Element element) {
        setBeanName(element);
				this.patternStr = element.getAttributeValue("pattern");
        this.pattern = new RegexRequestMatcher(patternStr, element.getAttributeValue("httpMethod"), Boolean.parseBoolean(element.getAttributeValue("caseInsensitive")));
        this.access = element.getAttributeValue("access");
    }

    @Override
    public Object update(ApplicationContext applicationContext, Properties properties, Object bean) {
        GeonetworkFilterSecurityInterceptor interceptor = (GeonetworkFilterSecurityInterceptor) bean;
        
        FilterInvocationSecurityMetadataSource metadataSource = interceptor.getSecurityMetadataSource();
        
        OverridesMetadataSource overrideSource;
        if (metadataSource instanceof OverridesMetadataSource) {
            overrideSource = (OverridesMetadataSource) metadataSource;
            
        } else {
            overrideSource = new OverridesMetadataSource(metadataSource);
            interceptor.setSecurityMetadataSource(overrideSource);
        }
        
        overrideSource.addMapping(patternStr, pattern, access);
        return null;
    }
}
