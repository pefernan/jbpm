/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.jbpm.process.builder;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.drools.compiler.builder.impl.KnowledgeBuilderImpl;
import org.drools.compiler.compiler.PackageRegistry;
import org.drools.compiler.lang.descr.ActionDescr;
import org.drools.compiler.rule.builder.PackageBuildContext;
import org.drools.compiler.rule.builder.dialect.mvel.MVELDialect;
import org.drools.core.WorkingMemory;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.definitions.impl.KnowledgePackageImpl;
import org.drools.core.rule.MVELDialectRuntimeData;
import org.drools.core.spi.ProcessContext;
import org.jbpm.process.builder.dialect.mvel.MVELActionBuilder;
import org.jbpm.process.instance.impl.Action;
import org.jbpm.process.instance.impl.MVELAction;
import org.jbpm.test.util.AbstractBaseTest;
import org.jbpm.workflow.core.DroolsAction;
import org.jbpm.workflow.core.impl.DroolsConsequenceAction;
import org.jbpm.workflow.core.node.ActionNode;
import org.junit.Test;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.runtime.StatefulKnowledgeSession;

public class MVELDecisionBuilderTest extends AbstractBaseTest {

    @Test
    public void testSimpleAction() throws Exception {
        final InternalKnowledgePackage pkg = new KnowledgePackageImpl( "pkg1" );

        ActionDescr actionDescr = new ActionDescr();
        actionDescr.setText( "list.add( 'hello world' )" );       

        KnowledgeBuilderImpl pkgBuilder = new KnowledgeBuilderImpl( pkg );
        
        PackageRegistry pkgReg = pkgBuilder.getPackageRegistry( pkg.getName() );
        MVELDialect mvelDialect = ( MVELDialect ) pkgReg.getDialectCompiletimeRegistry().getDialect( "mvel" );

        PackageBuildContext context = new PackageBuildContext();
        context.init( pkgBuilder, pkg, null, pkgReg.getDialectCompiletimeRegistry(), mvelDialect, null);
        
        pkgBuilder.addPackageFromDrl( new StringReader("package pkg1;\nglobal java.util.List list;\n") );        
        
        ActionNode actionNode = new ActionNode();
        DroolsAction action = new DroolsConsequenceAction("java", null);
        actionNode.setAction(action);
        
        final MVELActionBuilder builder = new MVELActionBuilder();
        builder.build( context,
                       action,
                       actionDescr,
                       actionNode );

        final KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages((Collection) Arrays.asList(pkgBuilder.getPackage()) );
        final StatefulKnowledgeSession wm = kbase.newStatefulKnowledgeSession();

        List<String> list = new ArrayList<String>();
        wm.setGlobal( "list", list );        
        
        MVELDialectRuntimeData data = (MVELDialectRuntimeData) pkgBuilder.getPackage().getDialectRuntimeRegistry().getDialectData( "mvel");
        
        ProcessContext processContext = new ProcessContext( ((InternalWorkingMemory) wm).getKnowledgeRuntime() );
        ((MVELAction) actionNode.getAction().getMetaData("Action")).compile( data );
        ((Action)actionNode.getAction().getMetaData("Action")).execute( processContext );
        
        assertEquals("hello world", list.get(0) );
    }    

}

