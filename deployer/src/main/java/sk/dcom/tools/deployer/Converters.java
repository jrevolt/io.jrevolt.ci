package sk.dcom.tools.deployer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
public class Converters {

    @TypeConverter
    static public class MvnUriConverter implements Converter<String, MvnUri> {
        @Override
        public MvnUri convert(String s) {
            return (StringUtils.trimToNull(s) == null) ? null : MvnUri.parse(s);
        }
    }

    @TypeConverter
    static public class InetSocketAddressConverter implements Converter<String, InetSocketAddress> {
        @Override
        public InetSocketAddress convert(String s) {
            try {
                String host = StringUtils.defaultIfEmpty(s.replaceAll(":.*", ""), "localhost");
                int port = Integer.parseInt(StringUtils.defaultIfEmpty(s.replaceAll("^[^:]+:", ""), "9999"));
                return new InetSocketAddress(InetAddress.getByName(host), port);
//			return InetSocketAddress.createUnresolved(host, port);
            } catch (UnknownHostException e) {
                throw new UnsupportedOperationException(e);
            }
        }
    }

    @Bean
    public ConversionService conversionService(Converter[] converters) {
        ConversionServiceFactoryBean bean = new ConversionServiceFactoryBean();
        bean.setConverters(new HashSet<>(Arrays.asList(converters)));
        bean.afterPropertiesSet();
        return bean.getObject();
    }

}