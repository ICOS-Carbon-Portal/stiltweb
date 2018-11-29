
export default class Marker{
	constructor(parent, element, leftPadding, markerOffset, dragEndCallback){
		this.parent = parent;
		this.element = element;
		this.markerOffset = markerOffset;
		this.dragEndCallback = dragEndCallback;

		this.getStyleLeft = x => x - leftPadding - markerOffset + 1;
		this.getClientX = x => x + leftPadding;
		this.mapPosition = x => x - leftPadding;
		this.edgePosition = x => x - markerOffset;

		this.isDragging = false;

		this.minX = -Infinity;
		this.maxX = Infinity;

		this.dragStartHandler = this.dragStart.bind(this);
		this.dragHandler = this.drag.bind(this);
		this.dragEndHandler = this.dragEnd.bind(this);

		this.addListeners();
	}

	set xRange(range){
		[this.minX, this.maxX] = range;
	}

	addListeners(){
		this.element.addEventListener("mousedown", this.dragStartHandler);
		this.parent.addEventListener("mousemove", this.dragHandler);
		this.element.addEventListener("mouseup", this.dragEndHandler);
		this.element.addEventListener("mouseout", this.mouseoutHandler);
	}

	release(){
		this.element.removeEventListener("mousedown", this.dragStartHandler);
		this.parent.removeEventListener("mousemove", this.dragHandler);
		this.element.removeEventListener("mouseup", this.dragEndHandler);
		this.element.removeEventListener("mouseout", this.mouseoutHandler);
	}

	mouseoutHandler() {
		if (!this.isDragging && this.dragEnd)
			this.dragEnd();
	}


	dragStart(e) {
		this.isDragging = true;
		this.element.style.left = this.getStyleLeft(e.clientX) + 'px';
	}

	drag(e) {
		if (!this.isDragging) return;

		const pos = this.mapPosition(e.clientX);

		if (pos <= this.minX){
			this.element.style.left = this.edgePosition(this.minX) + 'px';
			if (pos < this.minX - this.markerOffset * 2) this.dragEnd();

		} else if (pos >= this.maxX){
			this.element.style.left = this.edgePosition(this.maxX) + 'px';
			if (pos > this.maxX + this.markerOffset * 2) this.dragEnd();

		} else {
			this.element.style.left = this.getStyleLeft(e.clientX) + 'px';
		}
	}

	dragEnd(e) {
		if (!this.isDragging) return;

		this.isDragging = false;

		if (e) {
			this.dragEndCallback(e.clientX);

		} else {
			const left = parseFloat(this.element.style.left);
			const x = left - this.minX < this.maxX - left ? this.getClientX(this.minX) : this.getClientX(this.maxX);
			this.dragEndCallback(x);
		}
	}
}
