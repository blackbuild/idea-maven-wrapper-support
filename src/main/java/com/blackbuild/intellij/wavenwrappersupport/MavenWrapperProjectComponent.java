package com.blackbuild.intellij.wavenwrappersupport;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.maven.wrapper.DefaultDownloader;
import org.apache.maven.wrapper.Installer;
import org.apache.maven.wrapper.PathAssembler;
import org.apache.maven.wrapper.WrapperExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenGeneralSettings;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenServerManager;

import java.io.File;

public class MavenWrapperProjectComponent extends AbstractProjectComponent {

    private VirtualFile wrapperSettings;

    public MavenWrapperProjectComponent(Project project) {
        super(project);
    }

    void applyWrapper() {
        if (wrapperSettings == null)
            return;

        StringBuilder output = new StringBuilder(); // not actually used right now
        WrapperExecutor wrapperExecutor = WrapperExecutor.forWrapperPropertiesFile(new File(wrapperSettings.getPath()), output);
        Installer installer = new Installer(new DefaultDownloader("mvnw", "0.4.0"), new PathAssembler());

        File mavenHome;
        try {
            mavenHome = installer.createDist(wrapperExecutor.getConfiguration());
        } catch (Exception e) {
            PluginManager.getLogger().error(e);
            return;
        }

        MavenGeneralSettings generalSettings = MavenProjectsManager.getInstance(myProject).getGeneralSettings();
        if (generalSettings != null) {
            generalSettings.setMavenHome(mavenHome.getAbsolutePath());
            PluginManager.getLogger().info("Maven Instance set to Wrapper");
        }
    }

    void unapplyWrapper() {
        MavenGeneralSettings generalSettings = MavenProjectsManager.getInstance(myProject).getGeneralSettings();
        if (generalSettings != null) {
            generalSettings.setMavenHome(MavenServerManager.BUNDLED_MAVEN_3);
            PluginManager.getLogger().info("Maven Instance unset");
        }
    }

    class ChangeListener extends VirtualFileAdapter {

        @Override
        public void contentsChanged(@NotNull VirtualFileEvent event) {
            if (event.getFile().equals(wrapperSettings))
                applyWrapper();
        }

        @Override
        public void fileCreated(@NotNull VirtualFileEvent event) {
            if (wrapperSettings != null)
                return;
            if (!event.getFile().getPath().endsWith("/.mvn/wrapper/maven-wrapper.properties"))
                return;
            wrapperSettings = myProject.getBaseDir().findFileByRelativePath(".mvn/wrapper/maven-wrapper.properties");
        }

        @Override
        public void fileDeleted(@NotNull VirtualFileEvent event) {
            if (event.getFile().equals(wrapperSettings)) {
                wrapperSettings = null;
                unapplyWrapper();
            }
        }
    }

    @Override
    public void projectOpened() {
        VirtualFileManager.getInstance().addVirtualFileListener(new ChangeListener());
        wrapperSettings = myProject.getBaseDir().findFileByRelativePath(".mvn/wrapper/maven-wrapper.properties");
        applyWrapper();
    }

    @NotNull
    @Override
    public String getComponentName() {
        return this.getClass().getName();
    }
}
