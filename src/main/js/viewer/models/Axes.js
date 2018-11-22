import config from '../config';

const defaultVisible = ["co2.stilt", "co2.observed"];

export default class Axes{
	constructor(selectedScope, primary, secondary){
		this._ts = Date.now();
		this._selectedScope = selectedScope;
		this._primary = primary || this.getPrimary(selectedScope);
		this._secondary = secondary || this.getSecondary();

	}

	getPrimary(){
		const components = config.primaryComponents(this._selectedScope);
		return components.map(c => Object.assign(c, {visible: defaultVisible.includes(c.label)}));
	}

	getSecondary(){
		const columns = config.stiltResultColumns;
		const grouping = config.stiltResultColumnGrouping;

		return Object.keys(grouping).reduce((acc, key) => {
			const colList = grouping[key].map(label => columns.find(src => src.label === label));
			acc[key] = colList.map(c => Object.assign(c, {visible: defaultVisible.includes(c.label)}));
			return acc;
		}, {});
	}

	withSelectedScope(selectedScope){
		return new Axes(selectedScope, this._primary, this._secondary);
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

		return new Axes(this._selectedScope, primary, secondary);
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