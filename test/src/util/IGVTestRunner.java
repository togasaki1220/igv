/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */

package util;

import org.junit.Ignore;
import org.junit.experimental.categories.Category;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.io.IOException;

/**
 * Custom test runner, for categorizing/ignoring tests based on input flags.
 *
 * Because we depend on network resources, we don't necessarily want
 * to fail tests if they aren't available. Can set at launch whether we just log and ignore IOExceptions
 *
 * We can also annotate test methods with @LongRunning and skip those if desired.
 * User: jacob
 * Date: 2013-Feb-27
 */
@Ignore
public class IGVTestRunner extends BlockJUnit4ClassRunner {

    public static final String IGNORE_IOEXCEPTIONS_PROPERTY = "ignore.ioexceptions";
    public static final String INCLUDE_LONGRUNNING_PROPERTY = "include.longrunning";

    static boolean ignoreIOExceptions = false;
    static boolean includeLongRunning = false;

    static{
        ignoreIOExceptions = Boolean.parseBoolean(System.getProperty(IGNORE_IOEXCEPTIONS_PROPERTY, "" + ignoreIOExceptions));
        includeLongRunning = Boolean.parseBoolean(System.getProperty(INCLUDE_LONGRUNNING_PROPERTY, "" + includeLongRunning));
    }

    public static boolean isLongRunning(FrameworkMethod member){
        boolean isLongRunning = false;
        Category categoriesAnn = member.getAnnotation(Category.class);
        if(categoriesAnn != null){
            Class[] categories = categoriesAnn.value();
            for(Class category: categories){
                if(category.equals(LongRunning.class)){
                    isLongRunning = true;
                    break;
                }
            }
        }
        return isLongRunning;
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        boolean isLongRunning = isLongRunning(method);

        if(isLongRunning && !includeLongRunning){
            Description description = describeChild(method);
            notifier.fireTestIgnored(description);
        }else{
            super.runChild(method, notifier);
        }
    }

    @Override
    protected Statement methodInvoker(final FrameworkMethod method, Object test) {
        Statement statement =  super.methodInvoker(method, test);
        return new MethodInvoker(statement);
    }

    @Override
    protected Object createTest() throws Exception {
        return super.createTest();
    }

    public IGVTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    private static class MethodInvoker extends Statement{

        private Statement statement ;

        MethodInvoker(Statement statement){
            this.statement = statement;
        }

        @Override
        public void evaluate() throws Throwable {
            try{
                statement.evaluate();
            }catch (IOException e){
                if(ignoreIOExceptions){
                    e.printStackTrace();
                    throw new AssumptionViolatedException("IOException: " + e.getMessage());
                }else{
                    throw e;
                }
            }
        }
    }
}