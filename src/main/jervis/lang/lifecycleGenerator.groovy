package jervis.lang

import jervis.exceptions.UnsupportedLanguageException
import jervis.lang.lifecycleValidator
import org.yaml.snakeyaml.Yaml

class lifecycleGenerator {
    def jervis_yaml
    def language
    def lifecycle_obj
    def lifecycleGenerator() {
    }
    def loadLifecycles(URL url) {
        this.lifecycle_obj = new lifecycleValidator()
        this.lifecycle_obj.load_JSON(url)
        this.lifecycle_obj.validate()
    }
    def loadYaml(String raw_yaml) {
        if(!lifecycle_obj) {
            throw new Exception("bad time")
        }
        def yaml = new Yaml()
        this.jervis_yaml = yaml.load(raw_yaml)
        this.language = this.jervis_yaml['language']
        if(!lifecycle_obj.supportedLanguage(this.language)) {
            throw new UnsupportedLanguageException(this.language)
        }
    }
}
