/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.computerqueue;

import hudson.model.Action;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.Queue.BuildableItem;
import hudson.model.Queue.NotWaitingItem;
import hudson.model.Queue.WaitingItem;
import hudson.model.Slave;
import hudson.model.labels.LabelAtom;
import hudson.tasks.Shell;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import org.junit.Rule;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.JenkinsRule;

/**
 *
 * @author lucinka
 */
public class ScheduleMoreExecutorsJobTest extends HudsonTestCase{
    
    private ScheduleMoreExecutorsJob schedule = new ScheduleMoreExecutorsJob();
    
    private Project project1;
    private Project project2;
    private Project project3;
    private Project project4;
    private Project project5;
    private Project project6;
    private Slave slave1;
    private Slave slave2;
    
    private WaitingItem item1;
    private WaitingItem item2;
    private WaitingItem item3;
    private WaitingItem item4;
    private Queue.Item item5;
    private Queue.Item item6;
    
    @Override
    public void setUp() throws Exception{
        super.setUp();
        setObjects();
    }
    
    public void setObjects() throws IOException, Exception{
        project1 = createFreeStyleProject("project1");
        project2 = createFreeStyleProject("project2");
        project3 = createFreeStyleProject("project3");
        project4 = createFreeStyleProject("project4");
        project5 = createFreeStyleProject("project5");
        project6 = createFreeStyleProject("project6");
        slave1 = createOnlineSlave();
        slave2 = createOnlineSlave();
        project4.setAssignedLabel(new LabelAtom(slave2.getDisplayName()));
        project2.getBuildersList().add(new PutSlavesOfflineAction(slave2.getDisplayName()));
        project2.setAssignedLabel(new LabelAtom(slave1.getDisplayName()));
        project1.setAssignedLabel(new LabelAtom(slave1.getDisplayName()));
        project1.getBuildersList().add(new PutSlavesOfflineAction(slave2.getDisplayName()));
        project3.getBuildersList().add(new PutSlavesOfflineAction(slave1.getDisplayName()));
        project3.setAssignedLabel(new LabelAtom(slave2.getDisplayName()));
        project5.setAssignedLabel(null);
        project6.setAssignedLabel(jenkins.getLabel(slave1.getDisplayName() + "||" + slave2.getDisplayName()));
        GregorianCalendar calendar = new GregorianCalendar();
        item1 = new WaitingItem(calendar,project1,new ArrayList<Action>());
        GregorianCalendar calendar2 = new GregorianCalendar();
        calendar2.setTimeInMillis(calendar.getTimeInMillis()+1);
        item2 = new WaitingItem(calendar2,project2,new ArrayList<Action>());
        GregorianCalendar calendar3 = new GregorianCalendar();
        calendar3.setTimeInMillis(calendar2.getTimeInMillis()+1);
        item3 = new WaitingItem(calendar3,project3,new ArrayList<Action>());
        GregorianCalendar calendar4 = new GregorianCalendar();
        item4 = new WaitingItem(calendar3,project4,new ArrayList<Action>());
        calendar4.setTimeInMillis(calendar3.getTimeInMillis()+1);
        item5 = new WaitingItem(calendar4,project5,new ArrayList<Action>());
        item6 = new WaitingItem(calendar3,project6,new ArrayList<Action>());
    }
    

    public void testCanRunBefore() throws Exception{
        assertTrue("Method hasMorePriority() determines that older item berfore younger item",schedule.canRunBefore(item1, item2));
        assertFalse("Method hasMorePriority() determines the younger item can run before older item",schedule.canRunBefore(item2, item1));
        NotWaitingItem notWaiting1 = new BuildableItem(item1);
        NotWaitingItem notWaiting2 = new BuildableItem(item4);
        assertFalse("Method hasMorePriority() determines that 'waiting item' can run before 'not waiting item'",schedule.canRunBefore(item1, notWaiting2));
        assertTrue("Method hasMorePriority() determines that 'not waiting item' can not run before waiting item",schedule.canRunBefore(notWaiting1, item1));
        Thread.sleep(5);
        NotWaitingItem notWaiting3 = new BuildableItem(item4);
        assertTrue("Method hasMorePriority() determines that older 'not waiting item' can not run before younger 'not waiting item'",schedule.canRunBefore(notWaiting1, notWaiting3));
        assertFalse("Method hasMorePriority() determines that younger ' not waiting item' can run before older 'not waiting item' ",schedule.canRunBefore(notWaiting3, notWaiting1));
    }
    
    public void testContainsNodes(){
        List<Node> nodes = new ArrayList<Node>();
        assertTrue("Method canRunOnlyOnLabels() does not return true for the list of nodes which contains node, which is the only on which can execute given atomic label",schedule.canRunOnlyOnLabels(jenkins.getNodes(),jenkins.getLabel(slave1.getDisplayName())));
        assertTrue("Method canRunOnlyOnLabels() does not return true for the list of nodes which contains node, which is the only on which can execute given label experession",schedule.canRunOnlyOnLabels(jenkins.getNodes(),jenkins.getLabel(slave1.getDisplayName() + "||" + slave2.getDisplayName())));
        nodes.add(slave1);
        assertFalse("Method canRunOnlyOnLabels() does not return false if the list of node does not contain node which match given label", schedule.canRunOnlyOnLabels(nodes, new LabelAtom(slave2.getDisplayName())));
        assertTrue("Method canRunOnlyOnLabels() does not return true if the list of node contains node which match given label", schedule.canRunOnlyOnLabels(nodes, new LabelAtom(slave1.getDisplayName())));
    }
    

    public void testFindBlockingItem() throws IOException, Exception{
        List<Queue.Item> items = new ArrayList<Queue.Item>();
        items.add(item3);
        items.add(item2);
        assertNull("Method findBlockingItem() does not return null, if blocking items does not exist.",schedule.findBlockedItem(item2,items.toArray(new Queue.Item[2])));
        items.add(item1);
        Queue.Item item = schedule.findBlockedItem(item3,items.toArray(new Queue.Item[3]));
        assertNotNull("Method findBlockingItem() does not return non null blocking item object, if blocking items exists",item);
        assertTrue("Method findBlockingItem() does not return right blocking item.",item.equals(item1)|| item.equals(item2));
        item5 = new BuildableItem((WaitingItem)item5);     
        item6 = new BuildableItem((WaitingItem)item6);        
    }
    
    public void testIfNotInterruptAlreadyRunningJob() throws IOException, InterruptedException{
        project4.getBuildersList().add(new Shell("sleep 20"));
        jenkins.getQueue().schedule(project4,0);
        jenkins.getQueue().schedule(project2,1);
        Thread.sleep(1000);      
        assertTrue("Job + " + project2.getDisplayName() + " did not wait until a running job is finished", project2.isInQueue() && project4.isBuilding());
    }
    
    public void testIfLetRunOlderJob() throws IOException, InterruptedException{
        project4.getBuildersList().add(new Shell("sleep 1"));
        project5.setAssignedLabel(project4.getAssignedLabel());
        project5.getBuildersList().add(new Shell("sleep 20"));
        jenkins.getQueue().schedule(project4,0);
        jenkins.getQueue().schedule(project5,1);
        jenkins.getQueue().schedule(project2,2);
        while(project4.getLastBuild()==null || project4.getLastBuild().getResult()==null){
            Thread.sleep(1000);
        }  
        Thread.sleep(2000);
        assertTrue("Job + " + project2.getDisplayName() + " did not let older job run", project2.isInQueue() && (project5.isBuilding()||project5.getLastBuild().getResult()!=null));
    }
    
    public void testIfNotLetRunOlderJob() throws IOException, InterruptedException{
        project4.getBuildersList().add(new Shell("sleep 1"));
        project5.setAssignedLabel(project4.getAssignedLabel());
        project5.getBuildersList().add(new Shell("sleep 20"));
        jenkins.getQueue().schedule(project4,0);
        jenkins.getQueue().schedule(project2,1);
        jenkins.getQueue().schedule(project5,2);
        while(project4.getLastBuild()==null || project4.getLastBuild().getResult()==null){
            Thread.sleep(1000);
        }  
        assertTrue("Job + " + project2.getDisplayName() + " let younger job run", project5.isInQueue() && (project2.isBuilding()|| project2.getLastBuild().getResult()!=null));
    }
    
    public void testIfNotStuckWithTwoPutSlavesOfflineAction() throws IOException, InterruptedException{
        project4.getBuildersList().add(new Shell("sleep 5"));
        jenkins.getQueue().schedule(project4,0);
        project3.getBuildersList().add(new Shell("sleep 20"));
        jenkins.getQueue().schedule(project3,0);
        jenkins.getQueue().schedule(project2,1);
        while(project4.getLastBuild()==null || project4.getLastBuild().getResult()==null){
            Thread.sleep(1000);
        }
        Thread.sleep(2000);
        assertFalse("Jobs stuck in queue", project2.isInQueue() &&  project3.isInQueue());
        assertTrue("Job + " + project2.getDisplayName() + " let younger job run with PutSlaveOfflineAction", project2.isInQueue() && (project3.isBuilding()|| project3.getLastBuild().getResult()!=null));
    }
}
