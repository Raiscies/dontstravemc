# Project Instructions

## Minecraft Fabric Modding Guidelines

### Error Handling & Debugging

- When encountering compilation errors or missing symbols from external libraries (Minecraft, Geckolib, Cardinal Components), always fetch the actual source code instead of guessing
- For Fabric crash reports and Mixin application failures, analyze the error carefully before proposing solutions
- Use the fetch_source.py script to retrieve real implementations from library jars in loom-cache

### Working with External Libraries

**When you need to understand or reference external code:**

1. Identify the fully qualified class name from the error or requirement
   - Example: `net.minecraft.client.Minecraft` or `software.bernie.geckolib.animatable.GeoEntity`

2. Fetch the source code using:
   ```bash
   python .agent/skills/fabric-smart-debugger/scripts/fetch_source.py <FullyQualifiedClassName>
   ```

3. Read the extracted `.java` file from the printed path and base your solution on the actual implementation

### Mixin Development

- Always verify the target method signature by fetching the Minecraft source code before writing Mixin code
- Understand the actual implementation to ensure correct injection points and compatibility

### Code Style

- Follow existing project conventions for Java code
- Maintain consistency with Fabric modding patterns
- Keep business logic separate from rendering and client-side code

## Common Scenarios

- **Missing class/method errors**: Fetch source code to verify the correct API
- **Mixin failures**: Check the actual method implementation in Minecraft source
- **Library integration**: Review the library's actual code structure before implementing features
