# 从 Git 仓库中删除 server 目录

`server/` 目录已从工作区删除，若此前已推送到远程，需在本地从 Git 索引中移除并提交、推送，远程仓库中的 `server/` 才会消失。

---

## 步骤（在项目根目录执行）

### 1. 从 Git 索引中移除 server（保留本地已删文件的状态）

```bash
git rm -r --cached server
```

若你已手动删除了 `server/` 下所有文件，则用：

```bash
git rm -r server
```

（`git rm -r server` 会同时删除工作区中的 server 并从索引移除；若工作区已无 server，会报 “pathspec 'server' did not match any files”，此时用 `git add -u` 或 `git add .` 把“已删除”纳入暂存。）

**推荐**（无论 server 是否还在工作区）：

```bash
git add -A
git status
```

确认 `server` 下的文件显示为 “deleted”，然后提交。

### 2. 提交

```bash
git add .
git commit -m "chore: 删除未使用的 server 目录并更新计划文档引用"
```

（若前面已 `git rm -r server`，直接 `git commit -m "..."` 即可。）

### 3. 推送到远程

```bash
git push
```

推送后，远程仓库中的 `server/` 即被删除，其他人拉取后会看到该目录消失。

---

## 若希望从历史中彻底抹掉 server（慎用）

若希望从**整个 Git 历史**中删除 `server/`（重写历史），可使用 `git filter-branch` 或 `git filter-repo`。这会改变所有包含 server 的提交的 hash，其他人需要按新历史重新克隆或 rebase。仅在与团队沟通后、确定需要清理历史时再操作。此处不展开，需要时可查阅 “git filter-branch” 或 “git filter-repo” 文档。

---

*执行完“步骤 1～3”后，远程仓库中即不再包含 `server/` 目录。*
