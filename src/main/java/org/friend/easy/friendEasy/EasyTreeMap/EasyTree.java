package org.friend.easy.friendEasy.EasyTreeMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
public class EasyTree {
    public static class TreeNode<T> {
        private final T data;
        private final List<TreeNode<T>> children;

        public TreeNode(T data) {
            this.data = data;
            this.children = new ArrayList<>();
        }

        public TreeNode<T> addChild(T childData) {
            TreeNode<T> childNode = new TreeNode<>(childData);
            children.add(childNode);
            return childNode;
        }

        public List<TreeNode<T>> getChildren() {
            return children;
        }

        public T getData() {
            return data;
        }
    }

    public static <T> String printTree(TreeNode<T> root) {
        return printTree(root, Object::toString);
    }

    public static <T> String printTree(TreeNode<T> root, Function<T, String> dataFormatter) {
        if (root == null) return "";
        StringBuilder sb = new StringBuilder();
        formatNode(root, "", true, sb, dataFormatter, true);
        return sb.toString();
    }

    private static <T> void formatNode(TreeNode<T> node,
                                       String prefix,
                                       boolean isTail,
                                       StringBuilder sb,
                                       Function<T, String> formatter,
                                       boolean isRoot) {
        if (!isRoot) {
            sb.append(prefix)
                    .append(isTail ? "└── " : "├── ")
                    .append(formatter.apply(node.getData()))
                    .append("\n");
        } else {
            sb.append(formatter.apply(node.getData()))
                    .append("\n");
        }

        List<TreeNode<T>> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            boolean newIsTail = i == children.size() - 1;
            String newPrefix = isRoot ? "" : prefix + (isTail ? "    " : "│   ");
            formatNode(children.get(i),
                    newPrefix,
                    newIsTail,
                    sb,
                    formatter,
                    false);
        }
    }
//    public static void main(String[] args) {
//        TreeNode<String> root = new TreeNode<>("Root");
//
//        TreeNode<String> child1 = root.addChild("Child1");
//        child1.addChild("Grandchild1");
//        child1.addChild("Grandchild2");
//
//        TreeNode<String> child2 = root.addChild("Child2");
//        child2.addChild("Grandchild3");
//
//        TreeNode<String> child3 = root.addChild("Child3");
//
//        // 使用默认toString格式打印
//        System.out.println("Default format:");
//        System.out.println(printTree(root));
//
//        // 自定义格式打印
//        System.out.println("Custom format:");
//        System.out.println(printTree(root, data -> "[" + data + "]"));
//    }
}
