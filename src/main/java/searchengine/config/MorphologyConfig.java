package searchengine.config;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MorphologyConfig {

    @Bean
    public LuceneMorphology luceneMorphEn() throws Exception {
        return new EnglishLuceneMorphology();
    }

    @Bean
    public LuceneMorphology luceneMorphRu() throws Exception {
        return new RussianLuceneMorphology();
    }


}
