/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2018 Stephan Pauxberger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.blackbuild.intellij.wavenwrappersupport;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
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

    private Logger log = Logger.getInstance(this.getClass());

    private void applyWrapper() {
        if (wrapperSettings == null)
            return;

        StringBuilder output = new StringBuilder(); // not actually used by wrapper right now
        WrapperExecutor wrapperExecutor = WrapperExecutor.forWrapperPropertiesFile(new File(wrapperSettings.getPath()), output);
        File mavenUserHome = new File(System.getProperty("user.home") + "/.m2");
        Installer installer = new Installer(new DefaultDownloader("mvnw", "0.4.0"), new PathAssembler(mavenUserHome));

        try {
            File mavenHome = installer.createDist(wrapperExecutor.getConfiguration());
            changeMavenHomeTo(mavenHome.getAbsolutePath(), "maven wrapper defined in " + wrapperSettings.getPath());
        } catch (Exception e) {
            log.error(e);
        }
    }

    private void changeMavenHomeTo(String mavenPath, String message) {
        MavenGeneralSettings generalSettings = MavenProjectsManager.getInstance(myProject).getGeneralSettings();
        if (generalSettings == null)
            return;

        String oldMavenHome = generalSettings.getMavenHome();

        if (mavenPath.equals(oldMavenHome))
            return;

        generalSettings.setMavenHome(mavenPath);
        log.info("Maven changed to " + message);
        Notifications.Bus.notify(new Notification("maven-wrapper", "Maven changed", "Maven changed to " + message, NotificationType.INFORMATION));

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
            if (!myProject.isInitialized())
                return;
            wrapperSettings = myProject.getBaseDir().findFileByRelativePath(".mvn/wrapper/maven-wrapper.properties");
        }

        @Override
        public void fileDeleted(@NotNull VirtualFileEvent event) {
            if (event.getFile().equals(wrapperSettings)) {
                wrapperSettings = null;
                changeMavenHomeTo(MavenServerManager.BUNDLED_MAVEN_3, "Bundled Maven 3");
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
