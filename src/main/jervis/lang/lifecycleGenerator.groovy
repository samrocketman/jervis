package jervis.lang

import jervis.exceptions.UnsupportedLanguageException
import jervis.lang.lifecycleValidator
import jervis.lang.toolchainValidator
import jervis.tools.scmGit
import org.yaml.snakeyaml.Yaml

/**
  WIP: Docs will be written once this class has stabilized.
 */
class lifecycleGenerator {
    def jervis_yaml
    def language
    def lifecycle_obj
    def toolchain_obj
    def lifecycleGenerator() {
        scmGit git = new scmGit()
        this.loadLifecycles("${git.getRoot()}/src/resources/lifecycles.json")
        this.loadToolchains("${git.getRoot()}/src/resources/toolchains.json")
    }
    public void loadLifecycles(String file) {
        this.lifecycle_obj = new lifecycleValidator()
        this.lifecycle_obj.load_JSON(file)
        this.lifecycle_obj.validate()
    }
    public void loadToolchains(String file) {
        this.toolchain_obj = new toolchainValidator()
        this.toolchain_obj.load_JSON(file)
        this.toolchain_obj.validate()
    }
    /**
      Call loadLifecycles function before the loadYaml function.
     */
    public void loadYaml(String raw_yaml) {
        if(!lifecycle_obj) {
        }
        def yaml = new Yaml()
        this.jervis_yaml = yaml.load(raw_yaml)
        this.language = this.jervis_yaml['language']
        if(!lifecycle_obj.supportedLanguage(this.language)) {
            throw new UnsupportedLanguageException(this.language)
        }
    }
    public Boolean isMatrixBuild() {
    }
    public String excludeFilter() {
    }
    public String generateToolchainSection() {
    }
    public String generateBeforeInstall() {
    }
    public String generateInstall() {
    }
    public String generateBeforeScript() {
    }
    public String generateScript() {
    }
    public String generateAfterSuccess() {
    }
    public String generateAfterFailure() {
    }
    public String generateAfterScript() {
    }
    public String generateAll() {
    }
}
