# shelltree
A Java file cleanup utility. Allows defining file purge/archive rules per directory and executes them recursively. Can be used for cleaning up old data files, log files, etc.

shelltree is named based on the old DELTREE command in MS-DOS. DELTREE was used to remove a directory and all subdirectories regardless of what was in them. shelltree also processes a directory and all subdirectories, but only deletes files matching the specified filters and keep days per directory. 

Each directory has a hidden .shelltree file that contains the fules for file purging and archiving, as well as a recursive flag that indicates if child folders should inherit the same rules. If a child folder has its own .shelltree file then that will be used for that folder.

### Properties File
Properties file can have one of two names (depending on whether you want the file to be hidden):
- shelltree.properties
- .shelltree
  
Here is an example properties file:

    filePattern=*.log;*.txt;*.xml
    recursive=false
    fileAgeDays=30
    archiveFolder=archive
    archiveAgeDays=180

Properties:
- filePattern: a list of file/wildcard patterns to be purged. Default value is "*" (all files)
- recursive: a flag indicating whether to apply these rules to child folders (default=false)
- fileAgeDays: files older than this number of days will be purged (default=-1, meaning do not purge)
- archiveFolder: specifies the name of the subfolder to store archive .zip files in. Default=none (do not archive)
- archiveAgeDays: archive .zip files older than this number of days will be removed from the archive folder.

