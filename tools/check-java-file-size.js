#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const maxArg = process.argv.find((arg) => arg.startsWith("--max="));
const maxLines = maxArg ? Number(maxArg.slice("--max=".length)) : 500;
const root = path.join(__dirname, "..", "android", "app", "src", "main", "java");

function javaFiles(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...javaFiles(fullPath));
    } else if (entry.isFile() && entry.name.endsWith(".java")) {
      files.push(fullPath);
    }
  }
  return files;
}

const oversized = javaFiles(root)
  .map((file) => {
    const content = fs.readFileSync(file, "utf8");
    const lines = content.endsWith("\n") ? content.split("\n").length - 1 : content.split("\n").length;
    return { file, lines };
  })
  .filter((item) => item.lines > maxLines)
  .sort((left, right) => right.lines - left.lines);

if (oversized.length > 0) {
  console.error(`Java files must stay at or below ${maxLines} lines.`);
  for (const item of oversized) {
    console.error(`${item.lines} ${path.relative(process.cwd(), item.file)}`);
  }
  process.exit(1);
}

console.log(`Java file size check passed: max ${maxLines} lines.`);
