/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.enterprise.module.single;

import com.sun.enterprise.module.ModuleDefinition;
import com.sun.enterprise.module.ModuleMetadata;
import com.sun.enterprise.module.ModuleDependency;
import com.sun.enterprise.module.common_impl.ByteArrayInhabitantsDescriptor;
import com.sun.hk2.component.InhabitantsFile;

import java.util.jar.Manifest;
import java.util.Collections;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URI;
import java.io.File;

/**
 * Creates a ModuleDefinition backed up by a a single classloader
 *
 * @author Jerome Dochez
 */
public class ProxyModuleDefinition implements ModuleDefinition {

        private final ModuleMetadata metadata = new ModuleMetadata();
        private final Manifest manifest;

        public ProxyModuleDefinition(ClassLoader classLoader) throws IOException {
            this(classLoader, null, Collections.singleton("default"));
        }

        public ProxyModuleDefinition(ClassLoader classLoader, List<ManifestProxy.SeparatorMappings> mappings) throws IOException {
            this(classLoader, null, Collections.singleton("default"));
        }

        public ProxyModuleDefinition(ClassLoader classLoader, List<ManifestProxy.SeparatorMappings> mappings,
                                     Collection<String> habitatNames) throws IOException {
            manifest = new ManifestProxy(classLoader, mappings);
            for (String habitatName : habitatNames) {
                Enumeration<URL> inhabitants = classLoader.getResources(InhabitantsFile.PATH+'/'+habitatName);
                while (inhabitants.hasMoreElements()) {
                    URL url = inhabitants.nextElement();
                    metadata.addHabitat(habitatName,
                        new ByteArrayInhabitantsDescriptor(url, readFully(url))
                    );
                }
            }
        }

        private static byte[] readFully(URL url) throws IOException {
            DataInputStream dis=null;
            try {
                URLConnection con = url.openConnection();
                int len = con.getContentLength();
                InputStream in = con.getInputStream();
                dis = new DataInputStream(in);
                byte[] bytes = new byte[len];
                dis.readFully(bytes);
                return bytes;
            } catch (IOException e) {
                IOException x = new IOException("Failed to read " + url);
                x.initCause(e);
                throw x;
            } finally {
                if (dis!=null)
                    dis.close();
            }
        }

        public String getName() {
            return "Static Module";
        }

        public String[] getPublicInterfaces() {
            return new String[0];
        }

        public ModuleDependency[] getDependencies() {
            return EMPTY_MODULE_DEFINITIONS_ARRAY;
        }

        public URI[] getLocations() {
            return uris;
        }

        public String getVersion() {
            return "1.0.0";
        }

        public String getImportPolicyClassName() {
            return null;
        }

        public String getLifecyclePolicyClassName() {
            return null;
        }

        public Manifest getManifest() {
            return manifest;
        }

        public ModuleMetadata getMetadata() {
            return metadata;
        }

        private static boolean ok(String s) {
            return s != null && s.length() > 0;
        }

        private static boolean ok(String[] ss) {
            return ss != null && ss.length > 0;
        }
        private static final String[] EMPTY_STRING_ARRAY = new String[0];
        private static final ModuleDependency[] EMPTY_MODULE_DEFINITIONS_ARRAY = new ModuleDependency[0];
        private static URI[] uris = new URI[0];

    static {
        // It is impossible to change java.class.path after the JVM starts --
        // so cache away a copy...

        String cp = System.getProperty("java.class.path");
        if(ok(cp)) {
            String[] paths = cp.split(System.getProperty("path.separator"));

            if(ok(paths)) {
                uris = new URI[paths.length];

                for(int i = 0; i < paths.length; i++) {
                    uris[i] = new File(paths[i]).toURI();
                }
            }
        }
    }


    }
