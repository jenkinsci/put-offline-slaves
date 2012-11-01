/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.computerqueue;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Executor;
import hudson.model.Job;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.Queue.NotWaitingItem;
import hudson.model.Queue.WaitingItem;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueSorter;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.slaves.OfflineCause;
import hudson.slaves.OfflineCause.ByCLI;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;

/**
 *
 * @author lucinka
 */
@Extension
public class ScheduleMoreExecutorsJob extends QueueTaskDispatcher {

    @Override
    public CauseOfBlockage canRun(Queue.Item item) {
        PutSlavesOfflineAction step = null;
        if (item.task instanceof Project) {
            step = (PutSlavesOfflineAction) ((Project) item.task).getBuildersList().get(PutSlavesOfflineAction.class);
            if (step != null) {
                for (Node node : step.getNodes()) {
                    Queue.Item blockingItem = findBlockedItem(item, Jenkins.getInstance().getQueue().getItems());
                    if(blockingItem!=null)
                        return new JobBlockage("Waiting until job " + blockingItem.getDisplayName() + " on " + node.getDisplayName() + "is finished");
                    if(node.toComputer().isOnline()){
                        CauseOfBlockage blockage = takeOffline(node, item);
                        if(blockage !=null)
                            return blockage;
                    }
                    else{
                        if(node.toComputer().isIdle())
                            return null;
                        List<String> jobs = new ArrayList<String>();
                        for(Executor executor:node.toComputer().getExecutors()){
                            if(!executor.isIdle())
                                jobs.add(executor.getCurrentWorkUnit().context.item.task.getDisplayName());
                        }
                        return new JobBlockage("Waiting until running job on " + node.getDisplayName() + " is finished");
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Take slave temporary offline and determine if the item can run now or not
     * 
     * @param node which must be taken offline
     * @param item which want to run 
     * @return null if item can run now, otherwise instance of CauseOfBlockage (@link CauseOfBlockage) which inform about cause of blockage
     */
    public CauseOfBlockage takeOffline(Node node, Queue.Item item){
        OfflineCause blockage = new ByCLI("Slave was taken offline due to excuting of job " + item.task.getDisplayName());
        node.toComputer().setTemporarilyOffline(true, blockage);
        if(node.toComputer().isIdle())
            return null;
        return new JobBlockage("Waiting until previous job on " + node.getDisplayName() + " is finished");
    }
    
    /**
     * Find which item can be blocked by this item and return this "blocked item", if the item should run before this item.
     * 
     * @param item which can block another item.
     * @param items in which the blocked item is searched
     * @return one of items which will be blocked by this item, if that item should run before this item
     */
    public Queue.Item findBlockedItem(Queue.Item item, Queue.Item items[]){
        for(Queue.Item i:items){
            if(item.task.getDisplayName().equals(i.task.getDisplayName()))
                continue;
           if(i.task instanceof Project){
              Project project = (Project) item.task;
              PutSlavesOfflineAction thatStep = (PutSlavesOfflineAction) project.getBuildersList().get(PutSlavesOfflineAction.class);             
              if(thatStep!=null && canRunOnlyOnLabels(thatStep.getNodes(), i.task.getAssignedLabel())){
                  if(canRunBefore(i, item))
                      return i;
              }
                 
           }
            
        }
        return null;
    }
    
    /**
     * Determines if the given label can run only on one of given nodes right now.
     * 
     * @param nodes
     * @param label
     * @return true if nodes contains all online nodes on which job with this label can run, otherwise false
     */
    public boolean canRunOnlyOnLabels(List<Node> nodes, Label label){
    List<Node> nodesOfLabel = new ArrayList<Node>();
        if (label == null) {
            nodesOfLabel.addAll(Jenkins.getInstance().getNodes());
        }
        else{
            nodesOfLabel.addAll(label.getNodes());
        }
        List<Node> nodesRemoving = new ArrayList<Node>();
        for(Node node: nodesOfLabel){
            if(node.toComputer()==null || node.toComputer().isOffline()){ //item can not run on this slave now
               nodesRemoving.add(node);
            }
        }
        nodesOfLabel.removeAll(nodesRemoving);
        if(nodesOfLabel.size()>0 )
            return nodes.containsAll(nodesOfLabel);
        return false;
    }
    
    
    public boolean canRunBefore(Queue.Item item, Queue.Item comparedItem){
        if (comparedItem instanceof WaitingItem) {
                if (item instanceof WaitingItem) {
                   return ((WaitingItem) comparedItem).timestamp.getTimeInMillis() > ((WaitingItem) item).timestamp.getTimeInMillis();
                }
            }
            if (comparedItem instanceof NotWaitingItem) {
                if (item instanceof NotWaitingItem) 
                    return ((NotWaitingItem) comparedItem).buildableStartMilliseconds > ((NotWaitingItem) item).buildableStartMilliseconds;
                return false;
            }
        return true;
    }   

    public class JobBlockage extends CauseOfBlockage {

        private String message;

        public JobBlockage(String message) {
            this.message = message;
        }

        @Override
        public String getShortDescription() {
            return message;
        }
    }
}
