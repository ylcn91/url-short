"use client";

import * as React from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Copy, ExternalLink } from "lucide-react";
import { useToast } from "@/components/ui/use-toast";

interface UTMBuilderProps {
  baseUrl: string;
  onUrlChange?: (url: string) => void;
}

export function UTMBuilder({ baseUrl, onUrlChange }: UTMBuilderProps) {
  const { toast } = useToast();
  const [utmParams, setUtmParams] = React.useState({
    source: "",
    medium: "",
    campaign: "",
    term: "",
    content: "",
  });

  const generatedUrl = React.useMemo(() => {
    if (!baseUrl) return "";

    const url = new URL(baseUrl);
    const params = new URLSearchParams(url.search);

    if (utmParams.source) params.set("utm_source", utmParams.source);
    if (utmParams.medium) params.set("utm_medium", utmParams.medium);
    if (utmParams.campaign) params.set("utm_campaign", utmParams.campaign);
    if (utmParams.term) params.set("utm_term", utmParams.term);
    if (utmParams.content) params.set("utm_content", utmParams.content);

    url.search = params.toString();
    return url.toString();
  }, [baseUrl, utmParams]);

  React.useEffect(() => {
    if (onUrlChange && generatedUrl !== baseUrl) {
      onUrlChange(generatedUrl);
    }
  }, [generatedUrl, baseUrl, onUrlChange]);

  const copyToClipboard = async () => {
    await navigator.clipboard.writeText(generatedUrl);
    toast({
      title: "Copied!",
      description: "UTM URL copied to clipboard",
    });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>UTM Builder</CardTitle>
        <CardDescription>
          Add campaign tracking parameters to your URL
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid gap-4 md:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="utm_source">Campaign Source *</Label>
            <Input
              id="utm_source"
              placeholder="google, facebook, newsletter"
              value={utmParams.source}
              onChange={(e) => setUtmParams({ ...utmParams, source: e.target.value })}
            />
            <p className="text-xs text-muted-foreground">
              Where the traffic comes from (e.g., google, facebook)
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="utm_medium">Campaign Medium *</Label>
            <Input
              id="utm_medium"
              placeholder="cpc, email, social"
              value={utmParams.medium}
              onChange={(e) => setUtmParams({ ...utmParams, medium: e.target.value })}
            />
            <p className="text-xs text-muted-foreground">
              Marketing medium (e.g., cpc, email, social)
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="utm_campaign">Campaign Name *</Label>
            <Input
              id="utm_campaign"
              placeholder="summer_sale, product_launch"
              value={utmParams.campaign}
              onChange={(e) => setUtmParams({ ...utmParams, campaign: e.target.value })}
            />
            <p className="text-xs text-muted-foreground">
              Campaign identifier (e.g., summer_sale)
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="utm_term">Campaign Term</Label>
            <Input
              id="utm_term"
              placeholder="running+shoes"
              value={utmParams.term}
              onChange={(e) => setUtmParams({ ...utmParams, term: e.target.value })}
            />
            <p className="text-xs text-muted-foreground">
              Paid search keywords (optional)
            </p>
          </div>

          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="utm_content">Campaign Content</Label>
            <Input
              id="utm_content"
              placeholder="logolink, textlink"
              value={utmParams.content}
              onChange={(e) => setUtmParams({ ...utmParams, content: e.target.value })}
            />
            <p className="text-xs text-muted-foreground">
              Differentiate similar content or links (optional)
            </p>
          </div>
        </div>

        {generatedUrl && generatedUrl !== baseUrl && (
          <div className="space-y-2">
            <Label>Generated URL</Label>
            <div className="flex gap-2">
              <Input value={generatedUrl} readOnly className="font-mono text-sm" />
              <Button size="icon" variant="outline" onClick={copyToClipboard}>
                <Copy className="h-4 w-4" />
              </Button>
              <Button size="icon" variant="outline" asChild>
                <a href={generatedUrl} target="_blank" rel="noopener noreferrer">
                  <ExternalLink className="h-4 w-4" />
                </a>
              </Button>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
