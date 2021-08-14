import forohfor.scryfall.api.Set;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

public class SetLoadPanel extends JPanel implements MouseInputListener
{
    private static final long serialVersionUID = 1L;

    private RecognitionStrategy strat;

    private JTree tree;
    private DefaultMutableTreeNode root;

    private ImageIcon loaded = new ImageIcon("res/Loaded.png");
    private ImageIcon unloaded = new ImageIcon("res/Unloaded.png");
    private ImageIcon notdown = new ImageIcon("res/NotDownloaded.png");

    private ArrayList<SetSelectNode> allNodes = new ArrayList<SetSelectNode>();


    public SetLoadPanel(RecognitionStrategy st)
    {
        super();
        setLayout(new GridLayout(0, 1));

        strat = st;

        root = new DefaultMutableTreeNode("root");
        tree = new JTree(root);
        tree.addMouseListener(this);
        tree.setRootVisible(false);
        tree.setCellRenderer(new SetSelectTreeRenderer());
        add(tree);

        buildDisplayTree();
    }

    public void mousePressed(MouseEvent e)
    {
        Object sel = tree.getLastSelectedPathComponent();
        if (sel instanceof SetSelectNode)
        {
            SetSelectNode node = (SetSelectNode) tree.getLastSelectedPathComponent();
            if (e.getClickCount() == 2 && !e.isConsumed() && !node.isLoaded())
            {
                e.consume();
                if (node.fileExists())
                {
                    OperationBar bar = RecogApp.INSTANCE.getOpBar();
                    if (bar.setTask("Loading Sets", 1))
                    {
                        synchronized (strat)
                        {
                            try
                            {
                                strat.addFromFile(node.getFilePath());
                            } catch (Exception ex)
                            {
                                System.err.println("Couldn't load set -- exception follows");
                                ex.printStackTrace();
                            }
                            strat.finalizeLoad();
                            node.setLoaded(true);
                            bar.progressTask();
                        }
                    }
                } else
                {
                    downloadNode(node);
                }
            }
        }
    }

    public void actionPerformed(ActionEvent arg0)
    {
    }

    public int getBoxWidth()
    {
        return 50;
    }

    public void buildDisplayTree()
    {
        allNodes.clear();
        root.removeAllChildren();

        root.add(buildSetSubtree());
        root.add(buildFolderSubtree("Decks", "decks"));
        root.add(buildFolderSubtree("Custom Sets", "custom"));

        ((DefaultTreeModel) tree.getModel()).reload();
        if (this.getRootPane() != null)
        {
            this.getRootPane().validate();
        }

        loadSelected(true);
    }

    private DefaultMutableTreeNode buildSetSubtree()
    {
        DefaultMutableTreeNode subtree = new DefaultMutableTreeNode("Sets");

        ArrayList<SetSelectNode> nodes = new ArrayList<SetSelectNode>();
        HashMap<String, SetSelectNode> bySetCode = new HashMap<String, SetSelectNode>();

        ArrayList<Set> allSets = SetListing.getSets();

        for (Set set : allSets)
        {
            SetSelectNode node = new SetSelectNode(set);
            nodes.add(node);
            allNodes.add(node);
            bySetCode.put(set.getCode(), node);
        }
        Collections.sort(nodes);

        for (Set set : allSets)
        {
            String parent = set.getParentSetCode();
            if (parent != null)
            {
                bySetCode.get(set.getCode()).setParent(bySetCode.get(parent));
            }
        }

        for (SetSelectNode node : nodes)
        {
            if (!node.hasParent())
            {
                subtree.add(node);
            }
        }

        return subtree;
    }

    public DefaultMutableTreeNode buildFolderSubtree(String name, String folderName)
    {
        DefaultMutableTreeNode subtree = new DefaultMutableTreeNode(name);
        try
        {
            File[] flist = new File(SavedConfig.getSubPath(folderName)).listFiles();

            ArrayList<File> good = new ArrayList<>();
            for (int i = 0; i < flist.length; i++)
            {
                if (flist[i].isFile() && flist[i].getName().endsWith(".yam"))
                {
                    good.add(flist[i]);
                }
            }
            for (File f : good)
            {
                SetSelectNode node = new SetSelectNode(f);
                subtree.add(node);
                allNodes.add(node);
            }
        } catch (Exception e)
        {

        }

        return subtree;
    }

    @SuppressWarnings("unchecked")
    public void loadSelected()
    {
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        ArrayList<SetSelectNode> toLoad = new ArrayList<SetSelectNode>();

        if (selected != null)
        {
            Enumeration<TreeNode> nodes = selected.breadthFirstEnumeration();

            while (nodes.hasMoreElements())
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
                if (node instanceof SetSelectNode)
                {
                    SetSelectNode snode = (SetSelectNode) node;
                    if (snode.fileExists() && !snode.isLoaded())
                    {
                        toLoad.add(snode);
                    }
                }
            }
        }

        Counter c = new Counter();
        final OperationBar bar = RecogApp.INSTANCE.getOpBar();
        if (bar.setTask("Loading Sets", toLoad.size() + 1))
        {
            for (final SetSelectNode node : toLoad)
            {
                new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            synchronized (strat)
                            {
                                strat.addFromFile(node.getFilePath());
                                synchronized (c)
                                {
                                    c.count += 1;
                                    bar.progressTask();
                                    if (c.count == toLoad.size())
                                    {
                                        strat.finalizeLoad();
                                        bar.progressTask();
                                    }
                                }
                            }
                            node.setLoaded(true);
                            tree.repaint();
                        } catch (Exception e1)
                        {
                            e1.printStackTrace();
                            node.setLoaded(false);
                            bar.progressTask();
                        }
                    }
                }.start();

            }
        }
    }

    @SuppressWarnings("unchecked")
    public void loadSelected(boolean all)
    {
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if(all) {
            selected = (DefaultMutableTreeNode) tree.getModel().getRoot();
        }
        ArrayList<SetSelectNode> toLoad = new ArrayList<SetSelectNode>();

        if (selected != null)
        {
            Enumeration<TreeNode> nodes = selected.breadthFirstEnumeration();

            while (nodes.hasMoreElements())
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
                if (node instanceof SetSelectNode)
                {
                    SetSelectNode snode = (SetSelectNode) node;
                    if (snode.fileExists() && !snode.isLoaded())
                    {
                        toLoad.add(snode);
                    }
                }
            }
        }

        if(toLoad.size() == 0) {
            return;
        }

        Counter c = new Counter();
        final OperationBar bar = RecogApp.INSTANCE.getOpBar();
        if (bar.setTask("Loading Sets", toLoad.size() + 1))
        {
            for (final SetSelectNode node : toLoad)
            {
                new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            synchronized (strat)
                            {
                                strat.addFromFile(node.getFilePath());
                                synchronized (c)
                                {
                                    c.count += 1;
                                    bar.progressTask();
                                    if (c.count == toLoad.size())
                                    {
                                        strat.finalizeLoad();
                                        bar.progressTask();
                                    }
                                }
                            }
                            node.setLoaded(true);
                            tree.repaint();
                        } catch (Exception e1)
                        {
                            e1.printStackTrace();
                            node.setLoaded(false);
                            bar.progressTask();
                        }
                    }
                }.start();

            }
        }
    }

    public void downloadNode(SetSelectNode n)
    {
        int confirm = JOptionPane.showConfirmDialog(
                null,
                "Download set? This is time intensive and will halt recognition " +
                        "until the operation completes.", "Confirm set generation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null
        );

        if (confirm == JOptionPane.YES_OPTION)
        {
            final OperationBar bar = RecogApp.INSTANCE.getOpBar();
            if (bar.setTask("Downloading Set", 1))
            {
                new Thread()
                {
                    public void run()
                    {
                        bar.setSubtaskName(n.getSet().getName());
                        boolean downloaded = n.download();
                        if (downloaded)
                        {
                            tree.repaint();
                        }
                        bar.progressTask();
                    }
                }.start();
            }
        }
    }

    public void refresh()
    {
        if (unloadAll())
        {
            buildDisplayTree();
        }
    }

    public boolean unloadAll()
    {
        OperationBar bar = RecogApp.INSTANCE.getOpBar();
        if (bar.setTask("Unloading Sets", 1))
        {
            strat.clear();
            for (SetSelectNode node : allNodes)
            {
                node.setLoaded(false);
            }
            bar.progressTask();
            tree.repaint();
            return true;
        }
        return false;
    }

    @SuppressWarnings("serial")
    class SetSelectTreeRenderer extends DefaultTreeCellRenderer
    {
        @Override
        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean sel,
                boolean expanded, boolean leaf,
                int row, boolean hasFocus
        )
        {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof SetSelectNode)
            {
                SetSelectNode node = (SetSelectNode) value;

                if (node.fileExists())
                {
                    if (node.isLoaded())
                    {
                        label.setIcon(loaded);
                    } else
                    {
                        label.setIcon(unloaded);
                    }
                } else
                {
                    label.setIcon(notdown);
                }
            }
            return label;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
    }
}
