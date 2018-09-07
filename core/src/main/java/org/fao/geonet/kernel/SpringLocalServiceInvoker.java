/*
 * Copyright (C) 2001-2016 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */
package org.fao.geonet.kernel;

import org.fao.geonet.NodeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import java.util.Enumeration;
import java.io.ObjectInputStream;
import org.apache.commons.io.IOUtils;

import org.apache.commons.lang.StringUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;

public class SpringLocalServiceInvoker {

    @Autowired
    public RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    public RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @Autowired
    public NodeInfo nodeInfo;

    private HandlerMethodArgumentResolverComposite argumentResolvers;
    private HandlerMethodReturnValueHandlerComposite returnValueHandlers;
    private DefaultDataBinderFactory webDataBinderFactory;
    private String nodeId;

    public void init() {
        argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(requestMappingHandlerAdapter.getArgumentResolvers());
        returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(requestMappingHandlerAdapter.getReturnValueHandlers());
        webDataBinderFactory = new DefaultDataBinderFactory(requestMappingHandlerAdapter.getWebBindingInitializer());
        nodeId = nodeInfo.getId();
    }

    public Object invoke(String uri) throws Exception {
        MockHttpServletRequest request = prepareMockRequestFromUri(uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        return invoke(request, response);
    }

    public Object invoke(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Object o;
        String requestUrl = getUrl(request);
        if (requestUrl.startsWith("/xml.metadata.get?uuid=")) {
           o = invoke("/api/records/"+StringUtils.substringAfterLast(requestUrl,"="));
        } else {
          // this absolutely indecipherable spring shit fails with NPE on the second line
          // God knows why and unless you have grown up with spring you probably 
          // won't ever know
        	HandlerExecutionChain handlerExecutionChain = requestMappingHandlerMapping.getHandler(request);
        	HandlerMethod handlerMethod = (HandlerMethod) handlerExecutionChain.getHandler();
	
        	ServletInvocableHandlerMethod servletInvocableHandlerMethod = new ServletInvocableHandlerMethod(handlerMethod);
        	servletInvocableHandlerMethod.setHandlerMethodArgumentResolvers(argumentResolvers);
        	servletInvocableHandlerMethod.setHandlerMethodReturnValueHandlers(returnValueHandlers);
        	servletInvocableHandlerMethod.setDataBinderFactory(webDataBinderFactory);

        	o = servletInvocableHandlerMethod.invokeForRequest(new ServletWebRequest(request, response), null, new Object[0]);
        }
        // check whether we need to further process a "forward:" response
        if (o instanceof String) {
          String checkForward = (String)o;
          if (checkForward.startsWith("forward:")) {
            //
            // if the original url ends with the first component of the fwd url, then concatenate them, otherwise
            // just invoke it and hope for the best...
            // eg. local://srv/api/records/urn:marlin.csiro.au:org:1_organisation_name
            // returns forward:urn:marlin.csiro.au:org:1_organisation_name/formatters/xml
            // so we join the original url and the forwarded url as:
            // /api/records/urn:marlin.csiro.au:org:1_organisation_name/formatters/xml and invoke it.
            //
            String fwdUrl = StringUtils.substringAfter(checkForward,"forward:");
						String lastComponent = StringUtils.substringAfterLast(request.getRequestURI(),"/");
            if (lastComponent.length() > 0 && StringUtils.startsWith(fwdUrl, lastComponent)) {
							return invoke(request.getRequestURI()+StringUtils.substringAfter(fwdUrl,lastComponent));
						} else {
							return invoke(fwdUrl);	
           	} 
          }
        }
        return o;
    }

    /**
     * Get url from HttpServletRequest
     */
    public static String getUrl(HttpServletRequest request) {
        String requestUrl = request.getRequestURI().toString();
        StringBuffer queryString = new StringBuffer();
        final Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
          final String paramName = parameterNames.nextElement();
          final String paramValue = request.getParameter(paramName);
          queryString.append(paramName+"="+paramValue);
        }
        if (queryString.length() > 0) {
            requestUrl += "?"+queryString;
        }
        return requestUrl;
    }

    /**
     * prepareMockRequestFromUri will search for spring services that match
     * the request and execute them. Typically used for the local:// xlink
     * speed up. Accepts urls prefixed with local://<nodename> eg. 
     * local://srv/api/records/.. 
     * but also urls prefixed with the nodename only eg. '/srv/api/records/..'
     */
    private MockHttpServletRequest prepareMockRequestFromUri(String uri) {
        String requestURI = uri.replace("local:/","").replace("/"+nodeId, "").split("\\?")[0];
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
        request.setSession(new MockHttpSession());
        String[] splits = uri.split("\\?");
        if (splits.length > 1) {
            String params = splits[1];
            for (String param : params.split("&")) {
                String[] parts = param.split("=");
                String name = parts[0];
                request.addParameter(name, parts.length == 2 ? parts[1] : "");
            }
        }
        return request;
    }

  /**
	 * Prints the request.
	 *
	 * @param httpServletRequest the http servlet request
	 */
	private static void printRequest(final HttpServletRequest httpServletRequest) {
		if (httpServletRequest == null) {
			return;
		}
		System.out.println("----------------------------------------");
		System.out.println("W4 HttpServletRequest");
		System.out.println("\tRequestURL : {}"+ httpServletRequest.getRequestURL());
		System.out.println("\tRequestURI : {}"+ httpServletRequest.getRequestURI());
		System.out.println("\tScheme : {}"+ httpServletRequest.getScheme());
		System.out.println("\tAuthType : {}"+ httpServletRequest.getAuthType());
		System.out.println("\tEncoding : {}"+ httpServletRequest.getCharacterEncoding());
		System.out.println("\tContentLength : {}"+ httpServletRequest.getContentLength());
		System.out.println("\tContentType : {}"+ httpServletRequest.getContentType());
		System.out.println("\tContextPath : {}"+ httpServletRequest.getContextPath());
		System.out.println("\tMethod : {}"+ httpServletRequest.getMethod());
		System.out.println("\tPathInfo : {}"+ httpServletRequest.getPathInfo());
		System.out.println("\tProtocol : {}"+ httpServletRequest.getProtocol());
		System.out.println("\tQuery : {}"+ httpServletRequest.getQueryString());
		System.out.println("\tRemoteAddr : {}"+ httpServletRequest.getRemoteAddr());
		System.out.println("\tRemoteHost : {}"+ httpServletRequest.getRemoteHost());
		System.out.println("\tRemotePort : {}"+ httpServletRequest.getRemotePort());
		System.out.println("\tRemoteUser : {}"+ httpServletRequest.getRemoteUser());
		System.out.println("\tSessionID : {}"+ httpServletRequest.getRequestedSessionId());
		System.out.println("\tServerName : {}"+ httpServletRequest.getServerName());
		System.out.println("\tServerPort : {}"+ httpServletRequest.getServerPort());
		System.out.println("\tServletPath : {}"+ httpServletRequest.getServletPath());

		System.out.println("\tDispatcherType : {}"+ httpServletRequest.getDispatcherType());
		System.out.println("");

		System.out.println("\tHeaders");
		int j = 0;
		final Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			final String headerName = headerNames.nextElement();
			final String header = httpServletRequest.getHeader(headerName);
			System.out.println("\tHeader.name="+  headerName);
			System.out.println("\tHeader.value="+ header);
			j++;
		}

		System.out.println("\tLocalAddr : {}"+ httpServletRequest.getLocalAddr());
		System.out.println("\tLocale : {}"+ httpServletRequest.getLocale());
		System.out.println("\tLocalPort : {}"+ httpServletRequest.getLocalPort());

		System.out.println("");
		System.out.println("\tParameters");
		int k = 0;
		final Enumeration<String> parameterNames = httpServletRequest.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			final String paramName = parameterNames.nextElement();
			final String paramValue = httpServletRequest.getParameter(paramName);
			System.out.println("\tParam.name="+ paramName);
			System.out.println("\tParam.value="+ paramValue);
			k++;
		}

		System.out.println("");
		System.out.println("\tParts");
		int l = 0;
		try {
			for (final Object part : httpServletRequest.getParts()) {
				System.out.println("\tParts.class="+ part != null ? part.getClass() : "");
				System.out.println("\tParts.value={}"+ part != null ? part.toString() : "");
				l++;
			}
		} catch (final Exception e) {
			System.out.println("Exception "+ e);
		}

		try {
			System.out.println("Request Body : {}"+
					IOUtils.toString(httpServletRequest.getInputStream(), httpServletRequest.getCharacterEncoding()));
			System.out.println("Request Object : {}"+ new ObjectInputStream(httpServletRequest.getInputStream()).readObject());
		} catch (final Exception e) {
			System.out.println("Exception "+ e);
		}
		System.out.println("----------------------------------------");
	}

}
