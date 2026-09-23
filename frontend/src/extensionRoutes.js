export function matchExtension(extensions, pathname) {
  return extensions.find(({route})=>typeof route==='string' && (pathname===route || pathname.startsWith(route+'/')));
}
