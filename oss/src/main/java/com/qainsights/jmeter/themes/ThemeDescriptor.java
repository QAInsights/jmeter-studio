package com.qainsights.jmeter.themes;

/**
 * Describes a single theme — its identity, resource paths, and metadata.
 * Themes are loaded from themes.json manifests on the classpath.
 */
public class ThemeDescriptor {

    private String id;
    private String displayName;
    private String propertiesPath;
    private String iconDir;
    private String toolbarPropertiesPath;
    private String treeIconPropertiesPath;
    private boolean dark;
    private boolean pro;

    public ThemeDescriptor() {
    }

    public ThemeDescriptor(String id, String displayName, String propertiesPath,
                           String iconDir, String toolbarPropertiesPath,
                           String treeIconPropertiesPath, boolean dark, boolean pro) {
        this.id = id;
        this.displayName = displayName;
        this.propertiesPath = propertiesPath;
        this.iconDir = iconDir;
        this.toolbarPropertiesPath = toolbarPropertiesPath;
        this.treeIconPropertiesPath = treeIconPropertiesPath;
        this.dark = dark;
        this.pro = pro;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPropertiesPath() {
        return propertiesPath;
    }

    public void setPropertiesPath(String propertiesPath) {
        this.propertiesPath = propertiesPath;
    }

    public String getIconDir() {
        return iconDir;
    }

    public void setIconDir(String iconDir) {
        this.iconDir = iconDir;
    }

    public String getToolbarPropertiesPath() {
        return toolbarPropertiesPath;
    }

    public void setToolbarPropertiesPath(String toolbarPropertiesPath) {
        this.toolbarPropertiesPath = toolbarPropertiesPath;
    }

    public String getTreeIconPropertiesPath() {
        return treeIconPropertiesPath;
    }

    public void setTreeIconPropertiesPath(String treeIconPropertiesPath) {
        this.treeIconPropertiesPath = treeIconPropertiesPath;
    }

    public boolean isDark() {
        return dark;
    }

    public void setDark(boolean dark) {
        this.dark = dark;
    }

    public boolean isPro() {
        return pro;
    }

    public void setPro(boolean pro) {
        this.pro = pro;
    }

    @Override
    public String toString() {
        return "ThemeDescriptor{id='" + id + "', displayName='" + displayName + "', dark=" + dark + ", pro=" + pro + "}";
    }
}
