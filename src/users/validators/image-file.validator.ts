import { FileValidator } from '@nestjs/common';
import { IFile } from '@nestjs/common/pipes/file/interfaces';

export class ImageFileValidator extends FileValidator {
  // Accept only common static image types (no animated gifs)
  private readonly allowedMimeTypes = ['image/png', 'image/jpeg', 'image/webp'];

  constructor() {
    super({});
  }

  isValid(file?: IFile): boolean {
    if (!file) {
      return false;
    }
    return this.allowedMimeTypes.includes(file.mimetype);
  }

  buildErrorMessage(): string {
    return `File type must be one of: ${this.allowedMimeTypes.join(', ')}`;
  }
}

