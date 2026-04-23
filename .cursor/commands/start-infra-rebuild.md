# Start Infra Rebuild

執行下列動作，不要做其他步驟：

1. 切換工作目錄到 repo 根目錄下的 `infra/`
2. 執行：

```bash
./start -Rebuild
```

若要直接使用 `docker build`，請加上資源限制：

```bash
docker build --memory=10GB --cpus=12 -t <image-name> <build-context>
```
