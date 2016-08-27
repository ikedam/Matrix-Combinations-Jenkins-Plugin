/*
 * The MIT License
 * 
 * Copyright (c) 2014 IKEDA Yasuyuki
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.matrix_configuration_parameter;

import java.util.Arrays;

import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.TextAxis;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 *
 */
public class MatrixCombinationsParameterValueTest {
    @Rule
    public MatrixCombinationsJenkinsRule j = new MatrixCombinationsJenkinsRule();
    
    @Test
    public void testParametersPageWithSingleAxis() throws Exception{
        AxisList axes = new AxisList(new TextAxis("axis1", "value1", "value2", "value3"));
        MatrixProject p = j.createMatrixProject();
        p.setAxes(axes);
        p.addProperty(new ParametersDefinitionProperty(
                new MatrixCombinationsParameterDefinition("combinations", "", "axis1 != 'value2'")
        ));
        
        MatrixBuild b = p.scheduleBuild2(0).get();
        
        j.assertBuildStatusSuccess(b);
        
        WebClient wc = j.createWebClient();
        HtmlPage page = wc.getPage(b, "parameters");
        
        j.assertCombinationChecked(page, true, axes, "value1");
        j.assertCombinationChecked(page, false, axes, "value2");
        j.assertCombinationChecked(page, true, axes, "value3");
    }
    
    @Test
    public void testParametersPageWithDoubleAxis() throws Exception{
        AxisList axes = new AxisList(
                new TextAxis("axis1", "value1-1", "value1-2"),
                new TextAxis("axis2", "value2-1", "value2-2")
        );
        MatrixProject p = j.createMatrixProject();
        p.setAxes(axes);
        p.addProperty(new ParametersDefinitionProperty(
                new MatrixCombinationsParameterDefinition("combinations", "", "!(axis1 == 'value1-1' && axis2 == 'value2-2')")
        ));
        p.save();
        
        MatrixBuild b = p.scheduleBuild2(0).get();
        
        j.assertBuildStatusSuccess(b);
        
        WebClient wc = j.createWebClient();
        HtmlPage page = wc.getPage(b, "parameters");
        
        j.assertCombinationChecked(page, true, axes, "value1-1", "value2-1");
        j.assertCombinationChecked(page, false, axes, "value1-1", "value2-2");
        j.assertCombinationChecked(page, true, axes, "value1-2", "value2-1");
        j.assertCombinationChecked(page, true, axes, "value1-2", "value2-2");
    }
    
    @Bug(27233)
    @Test
    public void testNonMatrixBuild() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        
        @SuppressWarnings("deprecation")
        Cause cause = new Cause.UserCause();
        FreeStyleBuild b = p.scheduleBuild2(0, cause, Arrays.asList(
                new ParametersAction(new MatrixCombinationsParameterValue(
                        "combinations",
                        new Boolean[]{ true, false, true },
                        new String[]{ "axis1=value1", "axis1=value2", "axis1=value3" }
                ))
        )).get();
        
        WebClient wc = j.createWebClient();
        wc.getPage(b, "parameters");
    }
}
