import fs from 'fs';
import path from 'path';
import esbuild, { BuildOptions } from 'esbuild';
import copyStaticFiles from 'esbuild-copy-static-files';
import * as crypto from 'crypto';

const ROOT = path.resolve(path.dirname(process.argv[1]), '..');

if (!fs.existsSync(ROOT + '/private/key.txt')) {
    fs.writeFileSync(ROOT + '/private/key.txt', crypto.randomBytes(15).toString('base64')
        .replace(/=/g, '')
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
    );
}

build({
    entryPoints: [
        path.join(ROOT, 'src/index.ts'),
    ],
    bundle: true,
    sourcemap: true,
    minify: false,
    format: 'iife',
    outdir: path.join(ROOT, 'dist'),
    metafile: true,
    loader: {
        '.ttf': 'file',
        '.svg': 'file',
        '.woff': 'file',
        '.woff2': 'file',
        '.eot': 'file',
    },
    plugins: [
        copyStaticFiles({
            src: path.join(ROOT, 'static'),
            dest: path.join(ROOT, 'dist'),
            dereference: true,
            errorOnExist: false,
            recursive: true,
        }),
    ],
}, true, path.join(ROOT, 'dist/web-meta.json'));


async function build(opts: BuildOptions, startServer: boolean, metaFileName: string) {
    let mode = (process.argv[2] || '').substring(0, 1).toLowerCase() as 's' | 'w' | '';
    let ctx = await esbuild.context(opts);
    if (startServer && mode === 's') {
        let result = await ctx.serve({
            host: '127.0.0.1',
            port: 8080,
            servedir: path.join(ROOT, 'dist'),
        });
        console.log('Server running on:');
        console.log(`    http://${result.host}:${result.port}/`);
    } else if (mode !== '') {
        await ctx.watch();
    } else {
        let result = await ctx.rebuild();
        if (result.errors.length > 0) {
            console.error(result.errors);
        }
        if (result.warnings.length > 0) {
            console.error(result.warnings);
        }
        if (!result.errors.length && !result.warnings.length) {
            console.log('Build done.');
        }
        ctx.dispose();
        if (!mode && metaFileName) {
            fs.mkdirSync(path.dirname(metaFileName), { recursive: true });
            fs.writeFileSync(metaFileName, JSON.stringify(result.metafile, null, 4));
        }
    }
}
