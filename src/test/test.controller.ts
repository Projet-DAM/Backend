import { Controller, Get, UseGuards, Req } from '@nestjs/common';
import { ApiTags, ApiBearerAuth } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Subscription, SubscriptionDocument } from '../subscriptions/schemas/subscription.schema';

@ApiTags('Test')
@Controller('test')
export class TestController {
    constructor(
        @InjectModel(Subscription.name) private subscriptionModel: Model<SubscriptionDocument>,
    ) { }

    @Get('subscriptions-simple')
    @UseGuards(JwtAuthGuard)
    @ApiBearerAuth('JWT-auth')
    @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
    async getSubscriptionsSimple(@Req() req: any) {
        try {
            // Version ultra-simple sans populate
            const subs = await this.subscriptionModel.find({}).limit(5).lean().exec();

            return {
                success: true,
                count: subs.length,
                data: subs.map(s => ({
                    id: s._id.toString(),
                    childId: s.childId.toString(),
                    parentId: s.parentId.toString(),
                    offerId: s.offerId.toString(),
                    status: s.status,
                    paymentStatus: s.paymentStatus
                }))
            };
        } catch (error) {
            return {
                success: false,
                error: error.message,
                stack: error.stack
            };
        }
    }

    @Get('subscriptions-populated')
    @UseGuards(JwtAuthGuard)
    @ApiBearerAuth('JWT-auth')
    @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
    async getSubscriptionsPopulated(@Req() req: any) {
        try {
            const subs = await this.subscriptionModel
                .find({})
                .populate('childId', 'nom prenom')
                .populate('parentId', 'nom prenom')
                .populate('offerId', 'name price')
                .limit(5)
                .lean()
                .exec();

            return {
                success: true,
                count: subs.length,
                data: subs
            };
        } catch (error) {
            return {
                success: false,
                error: error.message,
                stack: error.stack
            };
        }
    }
import { Controller, Get, Res, NotFoundException } from '@nestjs/common';
import { ApiTags, ApiOperation } from '@nestjs/swagger';
import type { Response } from 'express';
import { existsSync, readdirSync } from 'fs';
import { join } from 'path';

@ApiTags('Testing')
@Controller()
export class TestController {
  
  @Get('test-image')
  @ApiOperation({ summary: 'Test image serving - displays a random uploaded image or creates test image' })
  testImage(@Res() res: Response) {
    const uploadsDir = join(__dirname, '..', '..', 'uploads');
    
    // Try to find any image in uploads directory
    const findFirstImage = (dir: string): string | null => {
      if (!existsSync(dir)) return null;
      
      const items = readdirSync(dir, { withFileTypes: true });
      
      for (const item of items) {
        const fullPath = join(dir, item.name);
        
        if (item.isDirectory()) {
          const found = findFirstImage(fullPath);
          if (found) return found;
        } else if (item.isFile() && /\.(jpg|jpeg|png|gif|webp)$/i.test(item.name)) {
          return fullPath.replace(uploadsDir, '').replace(/\\/g, '/');
        }
      }
      
      return null;
    };
    
    const imagePath = findFirstImage(uploadsDir);
    
    if (imagePath) {
      const publicUrl = `/uploads${imagePath}`;
      
      res.send(`
        <!DOCTYPE html>
        <html>
        <head>
          <title>Backend Image Test - SUCCESS</title>
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <style>
            body {
              font-family: Arial, sans-serif;
              max-width: 800px;
              margin: 50px auto;
              padding: 20px;
              background: #f5f5f5;
            }
            .success {
              background: #4CAF50;
              color: white;
              padding: 20px;
              border-radius: 8px;
              margin-bottom: 20px;
            }
            .card {
              background: white;
              padding: 20px;
              border-radius: 8px;
              box-shadow: 0 2px 4px rgba(0,0,0,0.1);
              margin-bottom: 20px;
            }
            img {
              max-width: 100%;
              height: auto;
              border: 2px solid #ddd;
              border-radius: 8px;
            }
            code {
              background: #f0f0f0;
              padding: 2px 6px;
              border-radius: 3px;
              font-family: monospace;
            }
            .url-box {
              background: #f9f9f9;
              padding: 15px;
              border-left: 4px solid #4CAF50;
              margin: 15px 0;
              word-break: break-all;
            }
            h1 { margin-top: 0; }
            .info { color: #666; font-size: 14px; }
          </style>
        </head>
        <body>
          <div class="success">
            <h1>✅ Backend Image Serving is Working!</h1>
            <p>If you can see this page and the image below, your backend is configured correctly.</p>
          </div>
          
          <div class="card">
            <h2>Test Image:</h2>
            <img src="${publicUrl}" alt="Test Image" />
          </div>
          
          <div class="card">
            <h2>Image URL:</h2>
            <div class="url-box">
              <strong>Public URL:</strong> <code>${publicUrl}</code>
            </div>
            <p class="info">
              This is the URL format your Android app should use. Replace the domain with your server's IP address.
            </p>
          </div>
          
          <div class="card">
            <h2>✓ What This Means:</h2>
            <ul>
              <li>✅ Express static file serving is working</li>
              <li>✅ The uploads folder is accessible</li>
              <li>✅ Your network configuration allows external access</li>
              <li>✅ CORS is properly configured</li>
            </ul>
          </div>
          
          <div class="card">
            <h2>🔧 For Android Integration:</h2>
            <p>In your Android app, construct image URLs like:</p>
            <div class="url-box">
              <code>http://YOUR_PC_IP:3000${publicUrl}</code>
            </div>
            <p class="info">
              Replace <code>YOUR_PC_IP</code> with your actual IP address from the server console logs.
            </p>
          </div>
          
          <div class="card">
            <h2>📱 Next Steps:</h2>
            <ol>
              <li>Note the IP address shown in the server console</li>
              <li>Update your Android app's <code>BASE_URL</code> to use that IP</li>
              <li>Ensure your phone is on the same WiFi network</li>
              <li>Test accessing this page from your phone's browser</li>
            </ol>
          </div>
        </body>
        </html>
      `);
    } else {
      // No images found, create a test response
      res.send(`
        <!DOCTYPE html>
        <html>
        <head>
          <title>Backend Image Test - No Images Found</title>
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <style>
            body {
              font-family: Arial, sans-serif;
              max-width: 800px;
              margin: 50px auto;
              padding: 20px;
              background: #f5f5f5;
            }
            .warning {
              background: #ff9800;
              color: white;
              padding: 20px;
              border-radius: 8px;
              margin-bottom: 20px;
            }
            .card {
              background: white;
              padding: 20px;
              border-radius: 8px;
              box-shadow: 0 2px 4px rgba(0,0,0,0.1);
              margin-bottom: 20px;
            }
            code {
              background: #f0f0f0;
              padding: 2px 6px;
              border-radius: 3px;
              font-family: monospace;
            }
            h1 { margin-top: 0; }
          </style>
        </head>
        <body>
          <div class="warning">
            <h1>⚠️ Backend is Running But No Images Found</h1>
            <p>The server is accessible, but there are no uploaded images yet.</p>
          </div>
          
          <div class="card">
            <h2>✓ Good News:</h2>
            <ul>
              <li>✅ Your backend server is running</li>
              <li>✅ This page loaded successfully</li>
              <li>✅ Network connectivity is working</li>
              <li>✅ You can access the server from this device</li>
            </ul>
          </div>
          
          <div class="card">
            <h2>📤 To Test Image Serving:</h2>
            <ol>
              <li>Upload an image using the API endpoint:
                <br><code>POST /uploads/messages/image</code>
              </li>
              <li>Or manually place a test image in:
                <br><code>uploads/messages/images/test.jpg</code>
              </li>
              <li>Refresh this page to see the test image</li>
            </ol>
          </div>
          
          <div class="card">
            <h2>🔍 Upload Directory:</h2>
            <p>Location: <code>${uploadsDir}</code></p>
            <p>Status: ${existsSync(uploadsDir) ? '✅ Exists' : '❌ Does not exist'}</p>
          </div>
          
          <div class="card">
            <h2>📖 API Documentation:</h2>
            <p>Visit <a href="/api">/api</a> to see Swagger documentation and upload a test image.</p>
          </div>
        </body>
        </html>
      `);
    }
  }
  
  @Get('test-upload-path')
  @ApiOperation({ summary: 'Debug upload paths - check for backslashes' })
  testUploadPath() {
    const uploadsDir = join(__dirname, '..', '..', 'uploads');
    
    const scanDirectory = (dir: string, level = 0): any => {
      if (!existsSync(dir)) {
        return { error: 'Directory does not exist', path: dir };
      }
      
      const items = readdirSync(dir, { withFileTypes: true });
      const result: any = {
        absolutePath: dir,
        hasBackslashes: dir.includes('\\'),
        normalizedPath: dir.replace(/\\/g, '/'),
        items: []
      };
      
      if (level < 3) { // Limit recursion depth
        items.forEach(item => {
          const fullPath = join(dir, item.name);
          const relativePath = fullPath.replace(uploadsDir, '').replace(/\\/g, '/');
          const publicUrl = `/uploads${relativePath}`;
          
          result.items.push({
            name: item.name,
            type: item.isDirectory() ? 'directory' : 'file',
            relativePath,
            publicUrl,
            hasBackslashes: relativePath.includes('\\'),
          });
        });
      }
      
      return result;
    };
    
    return {
      message: 'Upload directory structure and path validation',
      baseDirectory: uploadsDir,
      platform: process.platform,
      pathSeparator: require('path').sep,
      structure: scanDirectory(uploadsDir),
      recommendations: [
        'All public URLs should use forward slashes (/)',
        'Database should store paths like: /uploads/messages/images/file.jpg',
        'Do not store absolute Windows paths (C:\\...)',
        'Do not store backslashes in database',
      ]
    };
  }
}
