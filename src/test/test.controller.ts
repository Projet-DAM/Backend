import { Controller, Get, UseGuards, Req, Res, NotFoundException } from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Subscription, SubscriptionDocument } from '../subscriptions/schemas/subscription.schema';
import type { Response } from 'express';
import { existsSync, readdirSync } from 'fs';
import { join } from 'path';

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
  @ApiOperation({ summary: 'Get a simple list of subscriptions (limited to 5)' })
  async getSubscriptionsSimple(@Req() req: any) {
    try {
      const subs = await this.subscriptionModel.find({}).limit(5).lean().exec();
      return {
        success: true,
        count: subs.length,
        data: subs.map((s) => ({
          id: s._id.toString(),
          childId: s.childId.toString(),
          parentId: s.parentId.toString(),
          offerId: s.offerId.toString(),
          status: s.status,
          paymentStatus: s.paymentStatus,
        })),
      };
    } catch (error) {
      return { success: false, error: error.message };
    }
  }

  @Get('subscriptions-populated')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('JWT-auth')
  @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
  @ApiOperation({ summary: 'Get a populated list of subscriptions (limited to 5)' })
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
      return { success: true, count: subs.length, data: subs };
    } catch (error) {
      return { success: false, error: error.message };
    }
  }

  @Get('test-image')
  @ApiOperation({ summary: 'Test image serving - displays a random uploaded image or diagnostic info' })
  testImage(@Res() res: Response) {
    const uploadsDir = join(process.cwd(), 'uploads');

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
          <style>
            body { font-family: sans-serif; max-width: 800px; margin: 2rem auto; background: #f0f0f0; }
            .card { background: white; padding: 1.5rem; border-radius: 8px; margin-bottom: 1rem; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
            img { max-width: 100%; border-radius: 4px; }
            pre { background: #eee; padding: 0.5rem; overflow-x: auto; }
          </style>
        </head>
        <body>
          <div class="card" style="background: #4CAF50; color: white;"><h1>✅ Serving Working!</h1></div>
          <div class="card"><h2>Test Image:</h2><img src="${publicUrl}" /></div>
          <div class="card"><h2>Public URL:</h2><pre><code>${publicUrl}</code></pre></div>
        </body>
        </html>
      `);
    } else {
      res.send(`<h1>⚠️ No images found in ${uploadsDir}</h1>`);
    }
  }

  @Get('test-upload-path')
  @ApiOperation({ summary: 'Debug upload paths - check for backslashes and normalization' })
  testUploadPath() {
    const uploadsDir = join(process.cwd(), 'uploads');
    return {
      message: 'Upload directory structure and path validation',
      baseDirectory: uploadsDir,
      platform: process.platform,
      pathSeparator: require('path').sep,
      recommendations: [
        'All public URLs should use forward slashes (/)',
        'Do not store absolute Windows paths (C:\\...) in the database',
      ],
    };
  }
}
