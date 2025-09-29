/*
 * Copyright 2021 Elvis Hew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.elvishew.xlog.internal.printer.file.backup;

import com.elvishew.xlog.printer.file.backup.BackupStrategy2;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackupUtil {

  /**
   * Backup the logging file, shifting existing backups if needed (when maxBackupIndex has been
   * reached)
   *
   * @param loggingFile    the logging file
   * @param backupStrategy the strategy should be use when backing up
   */
  public static void backup(File loggingFile, BackupStrategy2 backupStrategy) {
    String loggingFileName = loggingFile.getName();
    String path = loggingFile.getParent();
    File backupFile;
    File nextBackupFile;
    int maxBackupIndex = backupStrategy.getMaxBackupIndex();
    if (maxBackupIndex > 0) {
      backupFile = new File(path, backupStrategy.getBackupFileName(loggingFileName, maxBackupIndex));
      if (backupFile.exists()) {
        // maxBackupIndex is already being used, overwrite the oldest file
        File oldestBackupFile = findOldestFile(path, maxBackupIndex);
        loggingFile.renameTo(oldestBackupFile);
      } else {
        // maxBackupIndex not reached yet; use the lowest available index for the backup file
        for (int i = 1; i <= maxBackupIndex; i++) {
          nextBackupFile = new File(path, backupStrategy.getBackupFileName(loggingFileName, i));
          if (!nextBackupFile.exists()) {
            loggingFile.renameTo(nextBackupFile);
            break;
          }
        }
      }
    } else if (maxBackupIndex == BackupStrategy2.NO_LIMIT) {
      for (int i = 1; i < Integer.MAX_VALUE; i++) {
        nextBackupFile = new File(path, backupStrategy.getBackupFileName(loggingFileName, i));
        if (!nextBackupFile.exists()) {
          loggingFile.renameTo(nextBackupFile);
          break;
        }
      }
    } else {
      // Illegal maxBackIndex, could not come here.
    }
  }

  private static File findOldestFile(String dirPath, int max) {
    File dir = new File(dirPath);
    if (dir == null || !dir.isDirectory())
      return null;

    Pattern pattern = Pattern.compile(".*\\.(\\d+)$");
    File oldestFile = null;

    for (File file : dir.listFiles()) {
      if (!file.isFile()) continue;

      Matcher matcher = pattern.matcher(file.getName());
      if (matcher.matches()) {
        int n = Integer.parseInt(matcher.group(1));
        if (n >= 1 && n <= max) {
          if (oldestFile == null || file.lastModified() < oldestFile.lastModified()) {
            oldestFile = file;
          }
        }
      }
    }

    return oldestFile;
  }


  /**
   * Check if a {@link BackupStrategy2} is valid, will throw a exception if invalid.
   *
   * @param backupStrategy the backup strategy to be verify
   */
  public static void verifyBackupStrategy(BackupStrategy2 backupStrategy) {
    int maxBackupIndex = backupStrategy.getMaxBackupIndex();
    if (maxBackupIndex < 0) {
      throw new IllegalArgumentException("Max backup index should not be less than 0");
    } else if (maxBackupIndex == Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Max backup index too big: " + maxBackupIndex);
    }
  }
}
