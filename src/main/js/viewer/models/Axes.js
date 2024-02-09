import config from '../config';

export default class Axes{
	constructor(gas, selectedScope, primary, secondary){
		this._gas = gas;
		this._defaultVisible = [".stilt", ".observed"].map(vsuff => this._gas + vsuff)
		this._ts = Date.now();
		this._selectedScope = selectedScope;
		this._primary = primary || this.getPrimary(selectedScope);
		this._secondary = secondary || this.getSecondary();
	}

	getPrimary(){
		const defVisible = this._defaultVisible
		const components = getPrimaryComponents(this._selectedScope, this._gas);
		return components.map(c => Object.assign(c, {visible: defVisible.includes(c.label)}));
	}

	getSecondary(){
		const columns = config.byTracer[this._gas].stiltResultColumns;
		const grouping = config.byTracer[this._gas].stiltResultColumnGrouping;
		const defVisible = this._defaultVisible

		return Object.keys(grouping).reduce((acc, key) => {
			const colList = grouping[key].map(label => columns.find(src => src.label === label));
			acc[key] = colList.map(c => Object.assign(c, {visible: defVisible.includes(c.label)}));
			return acc;
		}, {});
	}

	withSelectedScope(selectedScope){
		return new Axes(this._gas, selectedScope, this._primary, this._secondary);
	}

	updateVisibility(label, isChecked){
		const primary = this._primary.map(c =>
			Object.assign(c, {visible: c.label === label ? isChecked : c.visible})
		);

		const secondary = Object.keys(this._secondary).reduce((acc, key) => {
			acc[key] = this._secondary[key].map(c =>
				Object.assign(c, {visible: c.label === label ? isChecked : c.visible})
			);
			return acc;
		}, {});

		return new Axes(this._gas, this._selectedScope, primary, secondary);
	}

	isLabelOnPrimary(label){
		return this._primary.some(comp => comp.label === label);
	}

	get isSecondaryActive(){
		return Object.keys(this._secondary).some(group => this._secondary[group].some(col => col.visible));
	}

	get ts(){
		return this._ts;
	}

	get primary(){
		return this._primary;
	}

	get secondary(){
		return this._secondary;
	}
}

export function getPrimaryComponents(selectedScope, gas){
	const {stiltResultColumns, icosColumns} = config.byTracer[gas]
	const obsColumns = !selectedScope || selectedScope.dataObject
			? icosColumns.slice(1)
			: [Object.assign({}, icosColumns[1], {disabled: true})];
	return obsColumns.concat(stiltResultColumns.slice(1,3))
}
