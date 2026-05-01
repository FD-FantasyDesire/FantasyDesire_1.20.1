# 合成表系统测试说明

## 测试步骤

### 1. 编译项目
```bash
./gradlew build
```

### 2. 运行数据生成
```bash
./gradlew runData
```

### 3. 验证生成的文件
检查以下目录是否生成了合成表JSON文件：
```
src/generated/resources/data/fantasydesire/recipes/
```

应该包含以下文件：
- chikeflare.json
- smart_pistol_a.json
- smart_pistol_b.json
- crimson_scythe.json
- twin_blade_l.json
- twin_blade_r.json
- pure_snow.json
- ardor_blossom_star.json
- starless_night.json
- crucible.json
- gireiken.json
- over_cold_0.json
- over_cold_1.json
- over_cold_2.json
- over_cold_3.json

### 4. 运行游戏测试
```bash
./gradlew runClient
```

### 5. 游戏内测试
1. 打开创造模式物品栏，找到FantasyDesire标签页
2. 检查是否能看到所有的幻想刀
3. 尝试合成一把刀：
   - 获取一把损坏的SlashBlade（击杀100个敌人）
   - 准备合成材料（金锭等）
   - 在工作台中按照配方合成
4. 检查合成结果是否正确

## 常见问题

### 问题1：数据生成失败
**症状**：运行`./gradlew runData`时出现错误

**解决方案**：
1. 检查所有刀是否在`FantasySlashBladeBuiltInRegistry`中正确注册
2. 确保所有ResourceLocation正确
3. 检查导入语句是否完整

### 问题2：合成表JSON文件未生成
**症状**：`src/generated/resources/data/fantasydesire/recipes/`目录为空

**解决方案**：
1. 确认`DataGen.java`中已注册`FantasySlashBladeRecipeProvider`
2. 检查`FantasySlashBladeRecipeProvider.buildRecipes`方法是否正确实现
3. 查看控制台输出，确认数据生成过程是否完成

### 问题3：游戏中合成表不工作
**症状**：游戏内无法按照配方合成刀

**解决方案**：
1. 确认`FDRecipeSerializerRegistry`已在主类中注册
2. 检查合成表JSON文件的格式是否正确
3. 验证`blade`字段是否对应正确的刀定义
4. 检查`SlashBladeIngredient`的要求是否合理

### 问题4：合成结果不正确
**症状**：合成出的刀属性不符合预期

**解决方案**：
1. 检查`FantasySlashBladeDefinition`中的定义是否正确
2. 验证刀的状态合并逻辑
3. 确认附魔系统是否正常工作

## 验证清单

- [ ] 项目编译成功
- [ ] 数据生成成功
- [ ] 所有合成表JSON文件已生成
- [ ] 游戏启动成功
- [ ] 创造模式物品栏显示所有幻想刀
- [ ] 合成表在游戏中正常工作
- [ ] 合成结果正确
- [ ] 刀的状态正确合并
- [ ] 附魔系统正常工作

## 调试技巧

### 启用调试日志
在`config/slashblade-common.toml`中设置：
```toml
[general]
debug = true
```

### 查看合成表加载
在游戏中打开合成表界面，按F3+H查看详细信息

### 检查刀的状态
使用`/slashblade debug`命令查看刀的详细状态

## 性能优化

### 减少合成表数量
如果合成表过多，可以考虑：
1. 合并相似的合成表
2. 使用条件合成表
3. 优化配方模式

### 优化数据生成
1. 只在需要时运行数据生成
2. 使用增量生成
3. 缓存生成的数据

## 扩展功能

### 添加新刀的合成表
1. 在`FantasySlashBladeBuiltInRegistry`中注册新刀
2. 在`FantasySlashBladeRecipeProvider`中添加合成表代码
3. 运行数据生成
4. 测试合成表

### 自定义合成规则
可以修改以下类来自定义合成规则：
- `FantasySlashBladeShapedRecipe` - 合成逻辑
- `FantasySlashBladeShapedRecipeBuilder` - 构建器
- `RequestDefinition` - 状态要求

## 支持和反馈

如有问题，请：
1. 查看日志文件
2. 检查错误堆栈
3. 参考前置mod SlashBlade的文档
4. 在相关社区寻求帮助