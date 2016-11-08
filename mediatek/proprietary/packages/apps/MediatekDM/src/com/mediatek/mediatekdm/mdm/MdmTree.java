package com.mediatek.mediatekdm.mdm;

import android.util.Base64;

import com.mediatek.mediatekdm.mdm.MdmException.MdmError;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class MdmTree {
    public static class DFProperties {
        public static final int ACCESS_TYPE_ADD = 1;
        public static final int ACCESS_TYPE_COPY = 2;
        public static final int ACCESS_TYPE_DELETE = 4;
        public static final int ACCESS_TYPE_EXEC = 8;
        public static final int ACCESS_TYPE_GET = 16;
        public static final int ACCESS_TYPE_REPLACE = 32;
        public static final int ACCESS_TYPE_DEFAULT_BEHAVIOR = 255;
    }

    private static class NodeNameMatcher implements TreeManagerAgent.TreeIterator {
        public String result;
        private String mName;

        public NodeNameMatcher(String name) {
            MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                    "NodeNameMatcher created with name " + name);
            mName = name;
            result = null;
        }

        public int access(String uri, int mode) throws MdmException {
            MdmEngine.getLogger().logMsg(
                    MdmLogLevel.DEBUG,
                    "NodeNameMatcher.access() match " + uri + " with " + mName + ", mode is "
                            + mode);
            if (mode == TreeManagerAgent.TreeIterator.MODE_BEFORE
                    || mode == TreeManagerAgent.TreeIterator.MODE_LEAF) {
                String[] segments = uri.split("/");
                MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                        "last component of uri is " + segments[segments.length - 1]);
                // String.split() will ensure that there is at least one element in result array
                if (segments[segments.length - 1].equals(mName)) {
                    result = uri;
                    return 1;
                } else {
                    return 0;
                }
            } else {
                return 0;
            }
        }
    }

    public static void destroyAll() throws MdmException {
        synchronized (TreeManagerAgent.class) {
            TreeManagerAgent.getInstance().destroy();
            TreeManagerAgent.sInstance = null;
        }
    }

    /**
     * Internal representation for native tree manager agent.
     */
    private static class TreeManagerAgent {
        /*
         * NOTE: Sync MODE_* values with native enumeration TREE_WalkReason .
         */
        @SuppressWarnings("unused")
        public interface TreeIterator {
            int MODE_LEAF = 0;
            int MODE_BEFORE = 1;
            int MODE_AFTER = 2;

            /**
             * Access the node at uri.
     *
             * @param uri
             *        Uri to the node.
             * @param mode
             *        Access mode, the values can be one of MODE_LEAF, MODE_BEFORE an MODE_AFTER.
             * @param isLeaf
             *        Whether the node is a leaf node.
             * @return 0 to continue, others to abort.
             * @throws MdmException
             *         If error happens, implementation should throw MdmException.
             */
            int access(String uri, int mode) throws MdmException;
        }

        private static class SubtreeHandlerPair<E> implements Comparable<SubtreeHandlerPair<E>> {
            private E mHandler;
            private String mUri;

            public SubtreeHandlerPair(String uri, E handler) {
                mUri = uri;
                mHandler = handler;
            }

            public int compareTo(SubtreeHandlerPair<E> another) {
                return -(mUri.compareTo(another.mUri));
            }

            public E getHandler() {
                return mHandler;
            }

            public String getUri() {
                return mUri;
            }
        }

        private static TreeManagerAgent sInstance;
        static {
            System.loadLibrary("jni_mdm");
        }

        public static TreeManagerAgent getInstance() throws MdmException {
            synchronized (TreeManagerAgent.class) {
                if (sInstance == null) {
                    sInstance = new TreeManagerAgent();
                }
                return sInstance;
            }
        }

        private Map<String, NodeOnAddHandler> mOnAddHandlers;

        private Map<String, NodeOnDeleteHandler> mOnDeleteHandlers;

        private SortedSet<SubtreeHandlerPair<NodeOnAddHandler>> mSubtreeOnAddHandlers;

        private SortedSet<SubtreeHandlerPair<NodeOnDeleteHandler>> mSubtreeOnDeleteHandlers;

        private TreeManagerAgent() throws MdmException {
            boolean result = false;
            try {
                result = initialize();
            } catch (UnsatisfiedLinkError e) {
                MdmEngine.getLogger().logMsg(MdmLogLevel.ERROR, e.getMessage());
            }
            if (!result) {
                throw new MdmException(MdmError.INTERNAL);
            }

            mOnAddHandlers = new HashMap<String, NodeOnAddHandler>();
            mSubtreeOnAddHandlers = new TreeSet<SubtreeHandlerPair<NodeOnAddHandler>>();
            mOnDeleteHandlers = new HashMap<String, NodeOnDeleteHandler>();
            mSubtreeOnDeleteHandlers = new TreeSet<SubtreeHandlerPair<NodeOnDeleteHandler>>();
        }

        public String buildTndsString(String subtreeUri) throws MdmException {
            final StringBuilder sb = new StringBuilder(
                    "<MgmtTree xmlns='syncml:dmddf1.2'><VerDTD>1.2</VerDTD>");
            sb.append("<Man>").append(getStringValue("./DevInfo/Man")).append("</Man><Mod>")
                    .append(getStringValue("./DevInfo/Mod")).append("</Mod>");
            walkTree(subtreeUri, new TreeIterator() {
                @Override
                public int access(String uri, int mode) throws MdmException {
                    MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                            "buildTndsString.access: " + uri + ", " + mode);
                    if (mode == TreeIterator.MODE_BEFORE || mode == TreeIterator.MODE_LEAF) {
                        String[] parts = splitNameAndPath(uri);
                        sb.append("<Node><NodeName>").append(parts[1]).append("</NodeName><Path>")
                                .append(parts[0]).append("</Path>");
                    }
                    if (mode == TreeIterator.MODE_BEFORE) {
                        sb.append("<RTProperties>");
                        sb.append("<Type>");
                        sb.append("<DDFName>").append(getProperty(uri, "Type"))
                                .append("</DDFName>");
                        sb.append("</Type>");
                        sb.append("</RTProperties>");
                    } else if (mode == TreeIterator.MODE_LEAF) {
                        sb.append("<RTProperties>");
                        sb.append("<Type>");
                        sb.append("<MIME>").append(getProperty(uri, "Type")).append("</MIME>");
                        sb.append("</Type>");
                        sb.append("</RTProperties>");
                    }

                    if (mode == TreeIterator.MODE_LEAF) {
                        String format = getProperty(uri, "Format");
                        if (format != null && !format.equals("")) {
                            // Format
                            sb.append("<Format><").append(format).append("/></Format>");
                        }
                        // Value
                        if (format.equals("bin")) {
                            int length = getBinValue(uri, null);
                            MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                                    "Bin value length is " + length);
                            byte[] buffer = new byte[length];
                            length = getBinValue(uri, buffer);
                            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
                            sb.append("<Value>").append(encoded).append("</Value>");
                        } else {
                            sb.append("<Value>").append(getStringValue(uri)).append("</Value>");
                        }
                    } else if (mode == TreeIterator.MODE_BEFORE) {
                        // Format
                        sb.append("<Format><node/></Format>");
                    }

                    if (mode == TreeIterator.MODE_AFTER || mode == TreeIterator.MODE_LEAF) {
                        sb.append("</Node>");
                    }

                    return 0;
                }
            });
            sb.append("</MgmtTree>");
            return sb.toString();
        }

        public native boolean addInterior(String nodeUri, String type);

        public native boolean addLeaf(String nodeUri, String format, String type, byte[] data);

        public native boolean deleteNode(String nodeUri);

        public native void destroy();

        /* Execute node */
        public native boolean execNode(String nodeUri, byte[] data);

        public native int getBinValue(String nodeUri, byte[] buffer);

        public native boolean getBoolValue(String nodeUri);

        public native int getIntValue(String nodeUri) throws MdmException;

        public native String getProperty(String nodeUri, String propertyName);

        public native String getStringValue(String nodeUri);

        public native boolean initialize();

        /* Utilities */
        public native boolean isLeaf(String nodeUri) throws MdmException;

        public native String listChildren(String nodeUri) throws MdmException;

        /* Persistent storage */
        public native boolean readTree();

        public native void registerAdd(String nodeUri, NodeAddHandler handler) throws MdmException;

        public void registerAddSubTree(String nodeUri, NodeAddHandler handler) throws MdmException {

        }

        public native void registerDelete(String nodeUri, NodeDeleteHandler handler)
                throws MdmException;

        public native void registerDeleteSubTree(String nodeUri, NodeDeleteHandler handler)
                throws MdmException;

        public native boolean registerNodeExecHandler(String nodeUri, NodeExecuteHandler handler);

        /* Handler */
        public native boolean registerNodeIoHandler(String nodeUri, NodeIoHandler handler);

        /**
         * Register an handler which will be invoked after an node is added. This handler overrides
         * subtree handlers on its parent nodes.
     *
         * @param nodeUri
         * @param handler
         */
        public synchronized void registerOnAddHandler(String nodeUri, NodeOnAddHandler handler) {
            mOnAddHandlers.put(nodeUri, handler);
        }

        /**
         * Register an handler which will be invoked before an node is deleted. This handler
         * overrides subtree handlers on its parent nodes.
     *
         * @param nodeUri
         * @param handler
         */
        public synchronized void
                registerOnDeleteHandler(String nodeUri, NodeOnDeleteHandler handler) {
            mOnDeleteHandlers.put(nodeUri, handler);
        }

        /**
         * Register an handler which will be invoked after any node under nodeUri is added.
     *
         * @param nodeUri
         * @param handler
         */
        public synchronized void registerSubtreeOnAddHandler(String nodeUri,
                NodeOnAddHandler handler) {
            mSubtreeOnAddHandlers.add(new SubtreeHandlerPair<NodeOnAddHandler>(nodeUri, handler));
        }

        public synchronized void registerSubtreeOnDeleteHandler(String nodeUri,
                NodeOnDeleteHandler handler) {
            mSubtreeOnDeleteHandlers.add(new SubtreeHandlerPair<NodeOnDeleteHandler>(nodeUri,
                    handler));
        }

        public native void replaceBinValue(String nodeUri, byte[] value);

        public native void replaceBoolValue(String nodeUri, boolean value);

        public native void replaceIntValue(String nodeUri, int value);

        public native void replaceProperty(String nodeUri, String propertyName, String value);

        public native void replaceStringValue(String nodeUri, String value);

        public native void unregisterAdd(String nodeUri) throws MdmException;

        public native void unregisterAddSubTree(String nodeUri) throws MdmException;

        public native void unregisterDelete(String nodeUri) throws MdmException;

        public native void unregisterDeleteSubTree(String nodeUri) throws MdmException;

        public native boolean unregisterNodeExecHandler(String nodeUri);

        public native boolean unregisterNodeIoHandler(String nodeUri);

        /**
         * Unregister an handler.
     *
         * @param nodeUri
         */
        public synchronized void unregisterOnAddHandler(String nodeUri) {
            mOnAddHandlers.remove(nodeUri);
        }

        public synchronized void unregisterOnDeleteHandler(String nodeUri) {
            mOnDeleteHandlers.remove(nodeUri);
        }

        public synchronized void unregisterOnSubtreeDeleteHandler(String nodeUri) {
            SubtreeHandlerPair<NodeOnDeleteHandler> pair = null;
            for (SubtreeHandlerPair<NodeOnDeleteHandler> p : mSubtreeOnDeleteHandlers) {
                if (p.getUri().equals(nodeUri)) {
                    pair = p;
                    break;
                }
            }
            mSubtreeOnDeleteHandlers.remove(pair);
        }

        public synchronized void unregisterSubtreeOnAddHandler(String nodeUri) {
            SubtreeHandlerPair<NodeOnAddHandler> pair = null;
            for (SubtreeHandlerPair<NodeOnAddHandler> p : mSubtreeOnAddHandlers) {
                if (p.getUri().equals(nodeUri)) {
                    pair = p;
                    break;
                }
            }
            mSubtreeOnAddHandlers.remove(pair);
        }

        /* Iterating */
        public native void walkTree(String nodeUri, TreeIterator iterator) throws MdmException;

        public native boolean writeTree();

        /* Invoked by native TMA */
        private synchronized void nodeAdded(String uri) {
            PLLogger logger = MdmEngine.getLogger();
            logger.logMsg(MdmLogLevel.DEBUG, "[TREE] +MdmTree.nodeAdded(" + uri + ")");
            if (mOnAddHandlers.containsKey(uri)) {
                logger.logMsg(MdmLogLevel.DEBUG, "[TREE] invoke onAddHandler");
                mOnAddHandlers.get(uri).onAdd(uri);
            } else {
                for (SubtreeHandlerPair<NodeOnAddHandler> p : mSubtreeOnAddHandlers) {
                    if (uri.startsWith(p.getUri())) {
                        logger.logMsg(MdmLogLevel.DEBUG, "[TREE] invoke subtree onAddHandler");
                        p.getHandler().onAdd(uri);
                        break;
                    }
                }
            }
            logger.logMsg(MdmLogLevel.DEBUG, "[TREE] -MdmTree.nodeAdded()");
        }

        /* Invoked by native TMA */
        private synchronized void nodeDeleted(String uri) {
            PLLogger logger = MdmEngine.getLogger();
            logger.logMsg(MdmLogLevel.DEBUG, "[TREE] +MdmTree.nodeDeleted(" + uri + ")");
            if (mOnDeleteHandlers.containsKey(uri)) {
                logger.logMsg(MdmLogLevel.DEBUG, "[TREE] invoke onDeleteHandler");
                mOnDeleteHandlers.get(uri).onDelete(uri);
            } else {
                for (SubtreeHandlerPair<NodeOnDeleteHandler> p : mSubtreeOnDeleteHandlers) {
                    if (uri.startsWith(p.getUri())) {
                        logger.logMsg(MdmLogLevel.DEBUG, "[TREE] invoke subtree onDeleteHandler");
                        p.getHandler().onDelete(uri);
                        break;
                    }
                }
            }
            logger.logMsg(MdmLogLevel.DEBUG, "[TREE] +MdmTree.nodeDeleted()");
        }
    }

    public static final String URI_SEPERATOR = "/";

    public static String makeUri(String... segments) {
        return Utils.join(URI_SEPERATOR, segments);
    }

    /**
     * Split "./A/B/C" to { "./A/B/", "C" }
     *
     * @param uri
     * @return
     */
    public static String[] splitNameAndPath(String uri) {
        String[] result = new String[] { null, null };
        int cutPoint = uri.lastIndexOf(URI_SEPERATOR);
        if (cutPoint == -1) {
            // No separator found. Treat the whole uri as the node name.
            result[1] = uri;
        } else {
            result[0] = uri.substring(0, cutPoint);
            result[1] = uri.substring(cutPoint + 1, uri.length());
        }
        return result;
    }

    private TreeManagerAgent mTMA;

    public MdmTree() {
        try {
            mTMA = TreeManagerAgent.getInstance();
        } catch (MdmException e) {
            throw new Error(e);
        }
    }

    /**
     * Add a child leaf node to the DM tree.
     *
     * @param parentUri
     *        The full pathname to parent of the node to be added.
     * @param nodeName
     *        Name of the node to be added (Cannot be null).
     * @param format
     *        Format of node's data (e.g "chr", "int", etc.) If null, default format ("chr")will be
     *        set.
     * @param type
     *        MIME type of node's data or null.
     * @param data
     *        Node data.
     * @throws MdmException
     */
    public void addChildLeafNode(String parentUri, String nodeName, String format, String type,
            byte[] data) throws MdmException {
        if (!mTMA.addLeaf(parentUri + URI_SEPERATOR + nodeName, format, type, data)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    /**
     * Add an interior child node to the DM tree.
     *
     * @param parentUri
     *        The full pathname to parent of the node to be added.
     * @param nodeName
     *        Name of the node to be added (Cannot be null).
     * @param type
     *        A name representing the Device Description Framework (DDF) document describing the
     *        collection of nodes rooted at this node, or null.
     * @throws MdmException
     */
    public void addInteriorChildNode(String parentUri, String nodeName, String type)
            throws MdmException {
        if (!mTMA.addInterior(parentUri + URI_SEPERATOR + nodeName, type)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    /**
     * Add an interior node to the DM tree.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @param type
     *        A name representing the Device Description Framework (DDF) document describing the
     *        collection of nodes rooted at this node, or null.
     * @throws MdmException
     */
    public void addInteriorNode(String nodeUri, String type) throws MdmException {
        if (!mTMA.addInterior(nodeUri, type)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    /**
     * Add a child leaf node to the DM tree.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @param format
     *        Format of node's data (e.g "chr", "int", etc.) If null, default format ("chr")will be
     *        set.
     * @param type
     *        MIME type of node's data or null.
     * @param data
     *        Node data.
     * @throws MdmException
     */
    public void addLeafNode(String nodeUri, String format, String type, byte[] data)
            throws MdmException {
        if (!mTMA.addLeaf(nodeUri, format, type, data)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    /**
     * Delete a node from the DM tree.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @throws MdmException
     */
    public void deleteNode(String nodeUri) throws MdmException {
        if (!mTMA.deleteNode(nodeUri)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    /**
     * Find the first node with name under nodeUri recursively.
     *
     * @param nodeUri
     * @param name
     * @return Uri of the node found, or null if no match.
     * @throws MdmException
     */
    public String findFirstNodeByName(String nodeUri, String name) throws MdmException {
        NodeNameMatcher iterator = new NodeNameMatcher(name);
        mTMA.walkTree(nodeUri, iterator);
        return iterator.result;
    }

    /**
     * Get the ACL (Access Control List) property of a node. The node must exist.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @return the value of the ACL property of the node.
     * @throws MdmException
     */
    public String getACL(String nodeUri) throws MdmException {
        return getProperty(nodeUri, "ACL");
    }

    /*
     * Value operations
     */
    public int getBinValue(String nodeUri, byte[] buffer) throws MdmException {
        return mTMA.getBinValue(nodeUri, buffer);
    }

    public boolean getBoolValue(String nodeUri) throws MdmException {
        return mTMA.getBoolValue(nodeUri);
    }

    public int getIntValue(String nodeUri) throws MdmException {
        return mTMA.getIntValue(nodeUri);
    }

    /**
     * Get a property of a node. The node must exist.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @param propertyName
     *        The name of the property (Includes the following: 'Name', 'ACL', 'Format', 'Type',
     *        'Title', 'Size').
     * @return the value of the property of the node.
     * @throws MdmException
     */
    public String getProperty(String nodeUri, String propertyName) throws MdmException {
        String result = mTMA.getProperty(nodeUri, propertyName);
        if (null == result) {
            throw new MdmException(MdmError.INTERNAL);
        }
        return result;
    }

    public String getStringValue(String nodeUri) throws MdmException {
        return mTMA.getStringValue(nodeUri);
    }

    /**
     * Check whether the node at nodeUri is a leaf node.
     *
     * @param nodeUri
     *        Uri to the node to be checked.
     * @return True if it's a leaf, false otherwise.
     * @throws MdmException
     */
    public boolean isLeaf(String nodeUri) throws MdmException {
        return mTMA.isLeaf(nodeUri);
    }

    public String[] listChildren(String nodeUri) throws MdmException {
        try {
            String children = mTMA.listChildren(nodeUri);
            MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                    "[TREE] listChildren returns " + children);
            if (children == null || children.length() == 0) {
                MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                        "[TREE] Children is null or 0 children.");
                return new String[0];
            } else {
                MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                        "[TREE] listChildren length " + children.length());
                return children.split("/");
            }
        } catch (MdmException e) {
            if (e.getError() == MdmError.NODE_MISSING) {
                return new String[0];
            } else {
                throw e;
            }
        }
    }

    /**
     * Register a handler for ADD command on a node. The node must exist.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @param handler
     *        The handler used by the tree to add the registered node.
     * @throws MdmException
     */
    public void registerAdd(String nodeUri, NodeAddHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    /**
     * Register a handler for ADD command on a sub tree. The node must exist.
     *
     * @param nodeUri
     *        The full pathname of the sub tree root.
     * @param handler
     *        The handler used by the tree to add the registered node.
     * @throws MdmException
     */
    public void registerAddSubTree(String nodeUri, NodeAddHandler handler) throws MdmException {
        mTMA.registerAddSubTree(nodeUri, handler);
    }

    /**
     * Register a handler for DELETE command on a node. The node must exist.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @param handler
     *        The handler used by the tree to delete the registered node.
     * @throws MdmException
     */
    public void registerDelete(String nodeUri, NodeDeleteHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    /**
     * Register a handler for DELETE command on a node. The node must exist.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @param handler
     *        The handler used by the tree to delete the registered node.
     * @throws MdmException
     */
    public void registerDeleteSubTree(String nodeUri, NodeDeleteHandler handler)
            throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    /**
     * Register a handler for EXECUTE command on a node. The node must exist.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @param handler
     *        The handler used by the tree to execute the registered node.
     * @throws MdmException
     */
    public void registerExecute(String nodeUri, NodeExecuteHandler handler) throws MdmException {
        if (!mTMA.registerNodeExecHandler(nodeUri, handler)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    /**
     * Register an IO handler for GET and REPLACE commands on a node. The node must exist and must
     * be a leaf node.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @param handler
     *        The handler used by the tree to read and write the node's data.
     * @throws MdmException
     */
    public void registerNodeIoHandler(String nodeUri, NodeIoHandler handler) throws MdmException {
        if (!mTMA.registerNodeIoHandler(nodeUri, handler)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    public void registerOnAddHandler(String nodeUri, NodeOnAddHandler handler) throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[TREE] +MdmTree.registerOnAddHandler(" + nodeUri + ")");
        mTMA.registerOnAddHandler(nodeUri, handler);
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, "[TREE] -MdmTree.registerOnAddHandler()");
    }

    public void registerOnDeleteHandler(String nodeUri, NodeOnDeleteHandler handler)
            throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[TREE] +MdmTree.registerOnDeleteHandler(" + nodeUri + ")");
        mTMA.registerOnDeleteHandler(nodeUri, handler);
        MdmEngine.getLogger()
                .logMsg(MdmLogLevel.DEBUG, "[TREE] -MdmTree.registerOnDeleteHandler()");
    }

    public void registerPreExecNotify(NodePreExecuteHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    /**
     * Register an IO handler for GET and REPLACE commands on a sub tree. The node must exist.
     *
     * @param nodeUri
     *        The full pathname of the sub-tree root.
     * @param handler
     *        The handler used by the tree to read and write the node's data.
     * @throws MdmException
     */
    public void registerSubTreeIoHandler(String nodeUri, NodeIoHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    public void registerSubtreeOnAddHandler(String nodeUri, NodeOnAddHandler handler)
            throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[TREE] +MdmTree.registerSubtreeOnAddHandler(" + nodeUri + ")");
        mTMA.registerSubtreeOnAddHandler(nodeUri, handler);
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[TREE] -MdmTree.registerSubtreeOnAddHandler()");
    }

    public void registerSubtreeOnDeleteHandler(String nodeUri, NodeOnDeleteHandler handler)
            throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[TREE] +MdmTree.registerSubtreeOnDeleteHandler(" + nodeUri + ")");
        mTMA.registerSubtreeOnDeleteHandler(nodeUri, handler);
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[TREE] -MdmTree.registerSubtreeOnDeleteHandler()");
    }

    /**
     * Replace the ACL (Access Control List) property of a node. The node must exist.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @param newAcl
     *        new ACL for node.
     * @throws MdmException
     */
    public void replaceACL(String nodeUri, String newAcl) throws MdmException {
        replaceProperty(nodeUri, "ACL", newAcl);
    }

    public void replaceBinValue(String nodeUri, byte[] value) throws MdmException {
        mTMA.replaceBinValue(nodeUri, value);
    }

    public void replaceBoolValue(String nodeUri, boolean value) throws MdmException {
        mTMA.replaceBoolValue(nodeUri, value);
    }

    public void replaceIntValue(String nodeUri, int value) throws MdmException {
        mTMA.replaceIntValue(nodeUri, value);
    }

    /**
     * Replace a property of a node. The node must exist.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @param propertyName
     *        The name of the property (Includes the following: 'Name', 'ACL', 'Format', 'Type',
     *        'Title', 'Size').
     * @param value
     *        Property value.
     * @throws MdmException
     */
    public void replaceProperty(String nodeUri, String propertyName, String value)
            throws MdmException {
        mTMA.replaceProperty(nodeUri, propertyName, value);
    }

    public void replaceStringValue(String nodeUri, String value) throws MdmException {
        mTMA.replaceStringValue(nodeUri, value);
    }

    /**
     * Unregister a handler for ADD command on a node.
     *
     * @param nodeUri
     *        The URI where the handler to unregister is.
     * @throws MdmException
     */
    public void unregisterAdd(String nodeUri) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    /**
     * Unregister a handler for ADD command on a node.
     *
     * @param nodeUri
     *        The URI where the handler to unregister is.
     * @throws MdmException
     */
    public void unregisterAddSubTree(String nodeUri) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    /**
     * Unregister a handler for DELETE command on a node.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @throws MdmException
     */
    public void unregisterDelete(String nodeUri) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    /**
     * Unregister a handler for DELETE command on a node.
     *
     * @param nodeUri
     *        The full pathname of the node.
     * @throws MdmException
     */
    public void unregisterDeleteSubTree(String nodeUri) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    /**
     * Unregister a handler for EXECUTE command on a node.
     *
     * @param nodeUri
     *        The URI where the handler to unregister is.
     * @throws MdmException
     */
    public void unregisterExecute(String nodeUri) throws MdmException {
        if (!mTMA.unregisterNodeExecHandler(nodeUri)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    /**
     * Unregister an IO handler for GET and REPLACE commands on a node.
     *
     * @param nodeUri
     *        The URI where the handler to unregister is.
     * @throws MdmException
     */
    public void unregisterNodeIoHandler(String nodeUri) throws MdmException {
        if (!mTMA.unregisterNodeIoHandler(nodeUri)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    public void unregisterOnAddHandler(String nodeUri) throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[TREE] +MdmTree.unregisterOnAddHandler(" + nodeUri + ")");
        mTMA.unregisterOnAddHandler(nodeUri);
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, "[TREE] -MdmTree.unregisterOnAddHandler()");
    }

    public void unregisterOnDeleteHandler(String nodeUri) throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[TREE] +MdmTree.unregisterOnDeleteHandler(" + nodeUri + ")");
        mTMA.unregisterOnDeleteHandler(nodeUri);
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[TREE] -MdmTree.unregisterOnDeleteHandler()");
    }

    public void unregisterOnSubtreeDeleteHandler(String nodeUri) throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[TREE] +MdmTree.unregisterOnSubtreeDeleteHandler(" + nodeUri + ")");
        mTMA.unregisterOnSubtreeDeleteHandler(nodeUri);
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[TREE] -MdmTree.unregisterOnSubtreeDeleteHandler()");
    }

    /**
     * Unregister an IO handler for GET and REPLACE commands on a sub tree.
     *
     * @param nodeUri
     *        The URI where the handler to unregister is.
     * @throws MdmException
     */
    public void unregisterSubTreeIoHandler(String nodeUri) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    public void unregisterSubtreeOnAddHandler(String nodeUri) throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[TREE] +MdmTree.unregisterSubtreeOnAddHandler(" + nodeUri + ")");
        mTMA.unregisterSubtreeOnAddHandler(nodeUri);
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[TREE] -MdmTree.unregisterSubtreeOnAddHandler()");
    }

    /**
     * Write any modified tree data to persistent storage.
     *
     * @throws MdmException
     */
    public void writeToPersistentStorage() throws MdmException {
        if (!mTMA.writeTree()) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    public String buildTndsString(String subtreeUri) throws MdmException {
        return mTMA.buildTndsString(subtreeUri);
    }

    public static String getNodeName(String uri) {
        String[] parts = uri.split(URI_SEPERATOR);
        return parts[parts.length - 1];
    }
}
