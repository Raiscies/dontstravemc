---
name: fabric-smart-debugger
description: Use this skill for Minecraft Fabric modding tasks, especially when encountering compilation errors, missing symbols, or needing to understand external library code (Minecraft, Geckolib, Cardinal Components, etc.).
---

# Fabric Smart Debugger Skill

This skill assists with Minecraft Fabric development by analyzing errors and dynamically fetching source code from binary libraries (jars) in the `loom-cache` when needed.

## Capabilities

1.  **Analyze Fabric/Mixin Errors**: Understands common Fabric crash reports and Mixin application failures.
2.  **Fetch External Source Code**: Can locate and extract source files from Minecraft and library jars (like Geckolib, Cardinal Components) to provide accurate context.

## Instructions

### When to use

- When the user asks to "fix this error" related to a missing class or method in a library.
- When you need to see the implementation of a Minecraft method to correctly Mixin into it.
- When you need to see how an external library (Geckolib, Cardinal Components) implements a feature.

### How to use

1.  **Identify the Missing/Unknown Class**:
    - From the error message or user request, identify the fully qualified class name (e.g., `net.minecraft.client.Minecraft` or `software.bernie.geckolib.animatable.GeoEntity`).

2.  **Fetch Source Code**:
    - **DO NOT** guess the implementation.
    - Use the `fetch_source.py` script to get the real code.
    - **Command**: `python .agent/skills/fabric-smart-debugger/scripts/fetch_source.py <FullyQualifiedClassName>`
    - **Example**: `python .agent/skills/fabric-smart-debugger/scripts/fetch_source.py net.minecraft.client.Minecraft`

3.  **Read and Analyze**:
    - The script will print the path to the extracted `.java` file.
    - Use the `view_file` tool to read the content of that file.
    - Use this real code to inform your solution or explanation.

## Troubleshooting

- If the script says "Could not find .gradle/loom-cache directory", ensure you are running the command from the project root.
- If the class is not found, verify the spelling of the fully qualified name.
