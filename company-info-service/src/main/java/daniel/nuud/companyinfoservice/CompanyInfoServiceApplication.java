package daniel.nuud.companyinfoservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.blockhound.BlockHound;

@SpringBootApplication
public class CompanyInfoServiceApplication {

    static {
        BlockHound.builder()
                .allowBlockingCallsInside("java.security.SecureRandom", "nextBytes")
                .allowBlockingCallsInside("sun.security.provider.NativePRNG$RandomIO", "implNextBytes")
                .allowBlockingCallsInside("com.ongres.scram.client.ScramClient$Builder$1", "get")
                .allowBlockingCallsInside("com.fasterxml.jackson.databind.ObjectMapper", "findAndRegisterModules")
                .allowBlockingCallsInside("com.fasterxml.jackson.databind.Module", "setupModule")
                .allowBlockingCallsInside("java.util.ServiceLoader", "load")
                .allowBlockingCallsInside("java.util.ServiceLoader$LazyClassPathLookupIterator", "hasNext")
                .allowBlockingCallsInside("java.util.jar.JarFile", "getInputStream")
                .allowBlockingCallsInside("com.fasterxml.jackson.databind.util.ClassUtil", "getResourceAsStream")
                .allowBlockingCallsInside("com.fasterxml.jackson.databind.deser.DeserializerCache", "_createAndCacheValueDeserializer")
                .allowBlockingCallsInside("com.fasterxml.jackson.databind.deser.DeserializerCache", "hasValueDeserializerFor")
                .install();
    }

    public static void main(String[] args) {
        SpringApplication.run(CompanyInfoServiceApplication.class, args);
    }

}
