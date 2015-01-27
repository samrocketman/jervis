package jervis.remotes

interface JervisRemote {
    String getWebUrl();
    String getCloneUrl();
    List branches(String project);
    String getFile(String project, String file_path, String ref);
    String toString();
}
