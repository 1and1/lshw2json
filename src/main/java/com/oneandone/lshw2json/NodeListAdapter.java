/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oneandone.lshw2json;

import java.util.AbstractList;
import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Adapter for the List interface that adapts a NodeList.
 * @see NodeList
 * @see List
 * @author stephan
 */
public class NodeListAdapter extends AbstractList<Node> {
    private final NodeList nodeList;
    
    public NodeListAdapter(NodeList nodeList) {
        this.nodeList = nodeList;
    }

    @Override
    public Node get(int index) {
        return nodeList.item(index);
    }

    @Override
    public int size() {
        return nodeList.getLength();
    }
}
