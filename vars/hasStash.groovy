/**
  Returns true if a Pipeline stash with the given name exists for the current build,
  false otherwise.

  Storage layout matches {@link org.jenkinsci.plugins.workflow.flow.StashManager}:

  - Default / on-controller storage: {@code <buildDir>/stashes/<name>.tar.gz}
  - {@link org.jenkinsci.plugins.workflow.flow.StashManager.StashAwareArtifactManager}
    implementations (including Artifact Manager on S3 / {@code artifact-manager-s3},
    which uses {@code io.jenkins.plugins.artifact_manager_jclouds.JCloudsArtifactManager}):
    {@code <same prefix as artifacts>/stashes/<name>.tgz} next to the {@code artifacts/}
    prefix, exposed through {@link jenkins.util.VirtualFile}.

  Fully NonCPS; no agent necessary.

  Usage:
  {@code
  if (hasStash('my-stash')) {
      unstash 'my-stash'
  }
  }
  */

import hudson.model.Run
import jenkins.model.ArtifactManager
import jenkins.model.Jenkins
import jenkins.util.VirtualFile
import org.jenkinsci.plugins.workflow.flow.StashManager

@NonCPS
Boolean existsOnRun(Run run, String name) {
    Jenkins.checkGoodName(name)
    File local = new File(run.rootDir, "stashes/${name}.tar.gz")
    if(local.isFile()) {
        return true
    }
    ArtifactManager am
    try {
        am = run.pickArtifactManager()
        VirtualFile artRoot = am?.root()
        if(artRoot == null) {
            return false
        }
        VirtualFile base = artRoot.parent
        if(base == null) {
            return false
        }
        // JClouds / S3 stashes use .tgz; local StashManager uses .tar.gz — order avoids extra blob lookups on S3.
        List<String> extensions = (am instanceof StashManager.StashAwareArtifactManager) ? ['.tgz', '.tar.gz'] : ['.tar.gz', '.tgz']
        for(String ext in extensions) {
            VirtualFile candidate = base.child("stashes/${name}${ext}")
            try {
                if(candidate.exists()) {
                    return true
                }
            } catch(Exception ignored) {
                // VirtualFile on remote blob stores (e.g. S3) may throw I/O or runtime failures per blob probe.
            }
        }
    } catch(Exception ignored) {
        return false
    }
    false
}

@NonCPS
Boolean call(String name) {
    existsOnRun(currentBuild.rawBuild, name)
}
