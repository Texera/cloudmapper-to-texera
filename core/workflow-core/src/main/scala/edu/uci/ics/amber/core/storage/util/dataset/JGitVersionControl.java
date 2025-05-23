/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package edu.uci.ics.amber.core.storage.util.dataset;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JGitVersionControl {

  public static String initRepo(Path path) throws GitAPIException, IOException {
    File gitDir = path.resolve(".git").toFile();
    if (gitDir.exists()) {
      throw new IOException("Repository already exists at " + path);
    }
    // try-with-resource make sure the resource is released
    try (Git git = Git.init().setDirectory(path.toFile()).call()) {
      // Retrieve the default branch name
      Ref head = git.getRepository().exactRef("HEAD");
      if (head == null || head.getTarget() == null) {
        return null;
      }
      String refName = head.getTarget().getName();
      // HEAD should be in the form of 'ref: refs/heads/defaultBranchName'
      if (!refName.startsWith("refs/heads/")) {
        return null;
      }
      return refName.substring("refs/heads/".length());
    }
  }

  public static InputStream readFileContentOfCommitAsInputStream(Path repoPath, String commitHash, Path filePath) throws IOException {
    if (!filePath.startsWith(repoPath)) {
      throw new IllegalArgumentException("File path must be under the repository path.");
    }

    if (Files.isDirectory(filePath)) {
      throw new IllegalArgumentException("File path points to a directory, not a file.");
    }

    try (Repository repository = new FileRepositoryBuilder()
        .setGitDir(repoPath.resolve(".git").toFile())
        .build();
         RevWalk revWalk = new RevWalk(repository)) {

      RevCommit commit = revWalk.parseCommit(repository.resolve(commitHash));
      TreeWalk treeWalk =
          TreeWalk.forPath(repository, repoPath.relativize(filePath).toString(), commit.getTree());
      if (treeWalk == null) {
        throw new IOException("File not found in commit: " + filePath);
      }
      ObjectId objectId = treeWalk.getObjectId(0);
      ObjectLoader loader = repository.open(objectId);

      // Return the InputStream for caller to manage
      return loader.openStream();
    }
  }

  public static void readFileContentOfCommitAsOutputStream(Path repoPath, String commitHash, Path filePath, OutputStream outputStream) throws IOException {
    if (!filePath.startsWith(repoPath)) {
      throw new IllegalArgumentException("File path must be under the repository path.");
    }

    if (Files.isDirectory(filePath)) {
      throw new IllegalArgumentException("File path points to a directory, not a file.");
    }

    try (Repository repository = new FileRepositoryBuilder()
        .setGitDir(repoPath.resolve(".git").toFile())
        .build();
         RevWalk revWalk = new RevWalk(repository)) {

      RevCommit commit = revWalk.parseCommit(repository.resolve(commitHash));
      TreeWalk treeWalk =
          TreeWalk.forPath(repository, repoPath.relativize(filePath).toString(), commit.getTree());
      if (treeWalk == null) {
        throw new IOException("File not found in commit: " + filePath);
      }
      ObjectId objectId = treeWalk.getObjectId(0);
      ObjectLoader loader = repository.open(objectId);

      loader.copyTo(outputStream);
    }
  }

  public static Set<PhysicalFileNode> getRootFileNodeOfCommit(Path repoPath, String commitHash) throws Exception {
    Map<String, PhysicalFileNode> pathToFileNodeMap = new HashMap<>();
    Set<PhysicalFileNode> rootNodes = new HashSet<>();

    try (Repository repository = new FileRepositoryBuilder()
        .setGitDir(repoPath.resolve(".git").toFile())
        .build();
         RevWalk revWalk = new RevWalk(repository)) {
      ObjectId commitId = repository.resolve(commitHash);
      RevCommit commit = revWalk.parseCommit(commitId);

      try (TreeWalk treeWalk = new TreeWalk(repository)) {
        treeWalk.addTree(commit.getTree());
        treeWalk.setRecursive(false);

        while (treeWalk.next()) {
          Path fullPath = repoPath.resolve(treeWalk.getPathString());
          long size = 0;
          if (!treeWalk.isSubtree()) {
            // Get file size for non-directory entries
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);
            size = loader.getSize();
          }
          PhysicalFileNode currentNode = createOrGetNode(pathToFileNodeMap, repoPath, fullPath, size);

          if (treeWalk.isSubtree()) {
            treeWalk.enterSubtree();
          } else {
            // For files, we've already added them. Just ensure parent linkage is correct.
            ensureParentChildLink(pathToFileNodeMap, repoPath, fullPath, currentNode);
          }

          // For directories, also ensure they are correctly linked
          if (currentNode.isDirectory()) {
            ensureParentChildLink(pathToFileNodeMap, repoPath, fullPath, currentNode);
          }
        }
      }
    }

    // Extract root nodes
    pathToFileNodeMap.values().forEach(node -> {
      if (node.getAbsolutePath().getParent().equals(repoPath)) {
        rootNodes.add(node);
      }
    });

    return rootNodes;
  }

  private static PhysicalFileNode createOrGetNode(Map<String, PhysicalFileNode> map, Path repoPath, Path path, long size) {
    return map.computeIfAbsent(path.toString(), k -> new PhysicalFileNode(repoPath, path, size));
  }

  private static PhysicalFileNode createOrGetNode(Map<String, PhysicalFileNode> map, Path repoPath, Path path) {
    return map.computeIfAbsent(path.toString(), k -> new PhysicalFileNode(repoPath, path, 0));
  }

  private static void ensureParentChildLink(Map<String, PhysicalFileNode> map, Path repoPath, Path childPath, PhysicalFileNode childNode) {
    Path parentPath = childPath.getParent();
    if (parentPath != null && !parentPath.equals(repoPath)) {
      PhysicalFileNode parentNode = createOrGetNode(map, repoPath, parentPath);
      parentNode.addChildNode(childNode);
    }
  }

  public static void add(Path repoPath, Path filePath) throws IOException, GitAPIException {
    try (Git git = Git.open(repoPath.toFile())) {
      // Stage the file addition/modification
      git.add().addFilepattern(repoPath.relativize(filePath).toString()).call();
    }
  }

  public static void rm(Path repoPath, Path filePath) throws IOException, GitAPIException {
    try (Git git = Git.open(repoPath.toFile())) {
      git.rm().addFilepattern(repoPath.relativize(filePath).toString()).call(); // Stages the file deletion
    }
  }

  // create a commit, and return the commit hash
  public static String commit(Path repoPath, String commitMessage) throws IOException, GitAPIException {
    FileRepositoryBuilder builder = new FileRepositoryBuilder();
    try (Repository repository = builder.setGitDir(repoPath.resolve(".git").toFile())
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir() // scan up the file system tree
        .build()) {

      try (Git git = new Git(repository)) {
        // Commit the changes that have been staged
        RevCommit commit = git.commit().setMessage(commitMessage).call();

        // Return the commit hash
        return commit.getId().getName();
      }
    }
  }

  public static void discardUncommittedChanges(Path repoPath) throws IOException, GitAPIException {
    try (Repository repository = new FileRepositoryBuilder()
        .setGitDir(repoPath.resolve(".git").toFile())
        .build();
         Git git = new Git(repository)) {

      // Reset hard to discard changes in tracked files
      git.reset().setMode(ResetCommand.ResetType.HARD).call();

      // Clean the working directory to remove untracked files
      git.clean().setCleanDirectories(true).call();
    }
  }

  public static boolean hasUncommittedChanges(Path repoPath) throws IOException, GitAPIException {
    try (Repository repository = new FileRepositoryBuilder()
        .setGitDir(repoPath.resolve(".git").toFile())
        .readEnvironment()
        .findGitDir()
        .build();
         Git git = new Git(repository)) {

      Status status = git.status().call();
      return !status.isClean();
    }
  }

  public static String getDirectoryPathOfCommit(Path repoPath, String commitHash) throws IOException {
    try (Repository repository = new FileRepositoryBuilder()
            .setGitDir(repoPath.resolve(".git").toFile())
            .build();
         RevWalk revWalk = new RevWalk(repository)) {

      // The repository path is the directory path for the commit
      return repoPath.toAbsolutePath().toString();
    }
  }
}
