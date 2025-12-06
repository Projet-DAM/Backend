import { Controller, Get, Param, Res, NotFoundException } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiParam } from '@nestjs/swagger';
import { Response } from 'express';
import { existsSync, readdirSync, statSync } from 'fs';
import { join } from 'path';

@ApiTags('Diagnostics')
@Controller('diagnostics')
export class DiagnosticsController {
  
  @Get('uploads/check')
  @ApiOperation({ summary: 'Check uploads directory structure' })
  checkUploads() {
    const baseDir = join(__dirname, '..', '..', 'uploads');
    
    const scanDir = (dir: string, relativePath = ''): any => {
      if (!existsSync(dir)) {
        return { error: 'Directory does not exist', path: dir };
      }
      
      const items = readdirSync(dir);
      const result: any = {
        path: relativePath || '/',
        files: [],
        directories: []
      };
      
      items.forEach(item => {
        const fullPath = join(dir, item);
        const stats = statSync(fullPath);
        const itemRelativePath = relativePath ? `${relativePath}/${item}` : item;
        
        if (stats.isDirectory()) {
          result.directories.push({
            name: item,
            path: itemRelativePath,
            contents: scanDir(fullPath, itemRelativePath)
          });
        } else {
          result.files.push({
            name: item,
            size: stats.size,
            url: `/uploads/${itemRelativePath}`,
            fullPath: fullPath
          });
        }
      });
      
      return result;
    };
    
    return {
      baseDirectory: baseDir,
      exists: existsSync(baseDir),
      structure: scanDir(baseDir)
    };
  }

  @Get('uploads/test/:folder/:subfolder/:filename')
  @ApiOperation({ summary: 'Test if a specific file is accessible' })
  @ApiParam({ name: 'folder', example: 'messages' })
  @ApiParam({ name: 'subfolder', example: 'images' })
  @ApiParam({ name: 'filename', example: 'test.jpg' })
  testFile(
    @Param('folder') folder: string,
    @Param('subfolder') subfolder: string,
    @Param('filename') filename: string,
  ) {
    const filePath = join(__dirname, '..', '..', 'uploads', folder, subfolder, filename);
    const publicUrl = `/uploads/${folder}/${subfolder}/${filename}`;
    
    return {
      requestedFile: filename,
      expectedPath: filePath,
      exists: existsSync(filePath),
      publicUrl: publicUrl,
      instructions: `If exists=true, you should be able to access: http://localhost:3000${publicUrl}`
    };
  }

  @Get('network/info')
  @ApiOperation({ summary: 'Get network configuration info' })
  getNetworkInfo() {
    const os = require('os');
    const interfaces = os.networkInterfaces();
    const addresses: any[] = [];
    
    for (const name of Object.keys(interfaces)) {
      for (const iface of interfaces[name]) {
        if (iface.family === 'IPv4' && !iface.internal) {
          addresses.push({
            interface: name,
            address: iface.address,
            testUrl: `http://${iface.address}:3000/uploads/`
          });
        }
      }
    }
    
    return {
      hostname: os.hostname(),
      platform: os.platform(),
      addresses: addresses,
      instructions: 'Use one of these IP addresses in your Android app ApiConfig.BASE_URL_PHYSICAL'
    };
  }
}
