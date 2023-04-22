

export abstract class Label {

}

export class PointLabel extends Label {

}

export class LineLabel extends Label {

}


export class ImageArea {

    private width: number;
    private height: number;
    private data: Uint16Array;

    constructor(
        private parent: HTMLElement
    ) {

    }

    public setImageData(image: {data: Uint16Array, width: number, height: number}): void {
        this.data = image.data;
        this.width = image.width;
        this.height = image.height;
    }

    // TODO: methods to set brightness, contrast, gamma, etc. using wasm module (just preview)

    public addLabel(label: Label): void {
    }

    public filterLabels(): void {
    }

    public removeLabels(): void {
    }


}
