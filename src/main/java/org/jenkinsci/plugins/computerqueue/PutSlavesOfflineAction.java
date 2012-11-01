/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.computerqueue;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.tasks.Builder;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;



/**
 *
 * @author lucinka
 */
public class PutSlavesOfflineAction extends Builder{
    
    private List<Node> nodesToOffline = new ArrayList<Node>();
    
    @DataBoundConstructor
    public PutSlavesOfflineAction(String nodes){
        System.out.println(nodes);
        String nodeNames[] = nodes.split(" ");
        for(String name: nodeNames){
            Node node = Jenkins.getInstance().getNode(name);
            if(node!=null)
               nodesToOffline.add(node);
        }
    }
    
    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener){
        return true;
    }
    
    public String getStringNodes(){
        StringBuilder builder = new StringBuilder();
        for(Node node: nodesToOffline){
            builder.append(node.getDisplayName());
            builder.append(" ");
        }
        return builder.toString();
    }
    
    public List<Node> getNodes(){
        return nodesToOffline;
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<Builder>{

        @Override
        public String getDisplayName() {
            return "Put offline slaves";
        }
        
    }
    
}
