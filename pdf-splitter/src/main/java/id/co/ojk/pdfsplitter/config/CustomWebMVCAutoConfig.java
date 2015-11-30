package id.co.ojk.pdfsplitter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
@AutoConfigureAfter(DispatcherServletAutoConfiguration.class)
public class CustomWebMVCAutoConfig extends WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter{

  @Value("${static.res.folder}")
  private String resFolder;
  @Value("${images.subpath}")
  private String subPath;
  
  public void addResourceHandlers(ResourceHandlerRegistry registry){
    registry.addResourceHandler("/" + subPath + "/**").addResourceLocations(resFolder);

    super.addResourceHandlers(registry);
  }
}