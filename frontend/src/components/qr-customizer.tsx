"use client";

import * as React from "react";
import { QRCodeSVG } from "qrcode.react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Download, Image as ImageIcon } from "lucide-react";

interface QRCustomizerProps {
  value: string;
  defaultSize?: number;
}

export function QRCustomizer({ value, defaultSize = 256 }: QRCustomizerProps) {
  const [qrConfig, setQrConfig] = React.useState({
    size: defaultSize,
    fgColor: "#000000",
    bgColor: "#FFFFFF",
    level: "M" as "L" | "M" | "Q" | "H",
    includeMargin: true,
  });

  const downloadQR = (format: "svg" | "png") => {
    const svg = document.getElementById("qr-code-preview");
    if (!svg) return;

    if (format === "svg") {
      const svgData = new XMLSerializer().serializeToString(svg);
      const blob = new Blob([svgData], { type: "image/svg+xml" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `qr-code.svg`;
      link.click();
      URL.revokeObjectURL(url);
    } else {
      const canvas = document.createElement("canvas");
      const ctx = canvas.getContext("2d");
      if (!ctx) return;

      const img = new Image();
      const svgData = new XMLSerializer().serializeToString(svg);
      const blob = new Blob([svgData], { type: "image/svg+xml;charset=utf-8" });
      const url = URL.createObjectURL(blob);

      img.onload = () => {
        canvas.width = qrConfig.size;
        canvas.height = qrConfig.size;
        ctx.fillStyle = qrConfig.bgColor;
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.drawImage(img, 0, 0);

        canvas.toBlob((pngBlob) => {
          if (pngBlob) {
            const pngUrl = URL.createObjectURL(pngBlob);
            const link = document.createElement("a");
            link.href = pngUrl;
            link.download = `qr-code.png`;
            link.click();
            URL.revokeObjectURL(pngUrl);
          }
        });

        URL.revokeObjectURL(url);
      };

      img.src = url;
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>QR Code Customizer</CardTitle>
        <CardDescription>
          Customize and download your QR code
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="grid gap-6 md:grid-cols-2">
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="qr-size">Size (px)</Label>
              <Input
                id="qr-size"
                type="number"
                min="128"
                max="1024"
                step="64"
                value={qrConfig.size}
                onChange={(e) => setQrConfig({ ...qrConfig, size: parseInt(e.target.value) })}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="qr-fg-color">Foreground Color</Label>
              <div className="flex gap-2">
                <Input
                  id="qr-fg-color"
                  type="color"
                  value={qrConfig.fgColor}
                  onChange={(e) => setQrConfig({ ...qrConfig, fgColor: e.target.value })}
                  className="w-20 h-10"
                />
                <Input
                  type="text"
                  value={qrConfig.fgColor}
                  onChange={(e) => setQrConfig({ ...qrConfig, fgColor: e.target.value })}
                  className="flex-1"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="qr-bg-color">Background Color</Label>
              <div className="flex gap-2">
                <Input
                  id="qr-bg-color"
                  type="color"
                  value={qrConfig.bgColor}
                  onChange={(e) => setQrConfig({ ...qrConfig, bgColor: e.target.value })}
                  className="w-20 h-10"
                />
                <Input
                  type="text"
                  value={qrConfig.bgColor}
                  onChange={(e) => setQrConfig({ ...qrConfig, bgColor: e.target.value })}
                  className="flex-1"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="qr-level">Error Correction Level</Label>
              <Select
                value={qrConfig.level}
                onValueChange={(value: any) => setQrConfig({ ...qrConfig, level: value })}
              >
                <SelectTrigger id="qr-level">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="L">Low (7%)</SelectItem>
                  <SelectItem value="M">Medium (15%)</SelectItem>
                  <SelectItem value="Q">Quartile (25%)</SelectItem>
                  <SelectItem value="H">High (30%)</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                Higher levels allow the QR code to be read even when partially damaged
              </p>
            </div>

            <div className="flex gap-2">
              <Button onClick={() => downloadQR("svg")} className="flex-1 gap-2">
                <Download className="h-4 w-4" />
                Download SVG
              </Button>
              <Button onClick={() => downloadQR("png")} variant="outline" className="flex-1 gap-2">
                <ImageIcon className="h-4 w-4" />
                Download PNG
              </Button>
            </div>
          </div>

          <div className="flex items-center justify-center p-8 bg-muted rounded-lg">
            <div id="qr-code-preview" className="bg-white p-4 rounded-lg shadow-lg">
              <QRCodeSVG
                value={value}
                size={Math.min(qrConfig.size, 300)}
                fgColor={qrConfig.fgColor}
                bgColor={qrConfig.bgColor}
                level={qrConfig.level}
                includeMargin={qrConfig.includeMargin}
              />
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
