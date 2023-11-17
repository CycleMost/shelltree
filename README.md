# shelltree
A Java file cleanup utility. Allows defining file purge/archive rules per directory and executes them recursively. Can be used for cleaning up old data files, log files, etc.

shelltree is named based on the old DELTREE command in MS-DOS. DELTREE was used to remove a directory and all subdirectories regardless of what was in them. shelltree also processes a directory and all subdirectories, but only deletes files matching the specified filters and keep days per directory. 

Each directory has a hidden .shelltree file that contains the fules for file purging and archiving, as well as a recursive flag that indicates if child folders should inherit the same rules. If a child folder has its own .shelltree file then that will be used for that folder.

### TODO:
- Add up total# files and total file size per directory and grand totals
- Purge old archive files
- Optimize ZipFileSystem
